package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dummy.DummyCatalogData
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.Design
import com.example.data.repository.CatalogRepository
import com.example.util.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val categories: List<Category> = emptyList(),
    val subcategories: List<DummyCatalogData.SubcategoryItem> = DummyCatalogData.subcategories,
    val appSettings: AppSettings = AppSettings(),
    val allDesigns: List<Design> = emptyList(),
    val filteredDesigns: List<Design> = emptyList(),
    val pagedDesigns: List<Design> = emptyList(),
    val selectedCategorySlug: String = "all", // "all", or an admin category slug
    val selectedSubcategorySlug: String = "all", // "all", or an admin room slug
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = false,
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    /** Non-null when the designs stream failed (and no cached designs exist). */
    val designsError: String? = null,
    /** Non-null when the categories stream failed (and no cached categories exist). */
    val categoriesError: String? = null,
    val isOnline: Boolean = true,
    val isCheckingConnection: Boolean = false,
    val showOnlineRestoredBanner: Boolean = false,
    val toastMessage: String? = null
)

class CatalogViewModel(
    private val repository: CatalogRepository,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CatalogUiState(isOnline = networkMonitor?.isCurrentlyOnline() ?: true)
    )
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    // Tracked so retry can cancel a dead/failed stream and resubscribe
    // without stacking duplicate collectors.
    private var categoriesJob: Job? = null
    private var designsJob: Job? = null

    init {
        observeNetwork()
        loadData()
    }

    private fun observeNetwork() {
        val monitor = networkMonitor ?: return
        viewModelScope.launch {
            monitor.isOnline.collect { online ->
                val previousOnline = _uiState.value.isOnline
                _uiState.update { it.copy(isOnline = online) }

                if (online && !previousOnline) {
                    // Connection restored from offline state
                    _uiState.update {
                        it.copy(
                            showOnlineRestoredBanner = true,
                            toastMessage = "✅ ইন্টারনেট সংযোগ পুনঃস্থাপিত হয়েছে!"
                        )
                    }
                    // Auto-refresh on reconnection: re-check defaults, then
                    // resubscribe the content streams (failed collectors restart).
                    viewModelScope.launch { repository.ensureRemoteDefaults() }
                    retryLoading()

                    // Auto-hide the "Restored" banner after 3.5 seconds
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3500)
                        _uiState.update { it.copy(showOnlineRestoredBanner = false) }
                    }
                } else if (!online) {
                    _uiState.update {
                        it.copy(
                            showOnlineRestoredBanner = false,
                            toastMessage = "⚠️ ইন্টারনেট সংযোগ বিচ্ছিন্ন! এই অ্যাপটি ব্যবহারের জন্য ইন্টারনেট প্রয়োজন।"
                        )
                    }
                }
            }
        }
    }

    fun dismissOnlineRestoredBanner() {
        _uiState.update { it.copy(showOnlineRestoredBanner = false) }
    }

    fun retryConnection() {
        _uiState.update { it.copy(isCheckingConnection = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(600) // slight debounce for smooth UI interaction
            val isNowOnline = networkMonitor?.isCurrentlyOnline() ?: true
            _uiState.update {
                it.copy(
                    isOnline = isNowOnline,
                    isCheckingConnection = false
                )
            }

            if (isNowOnline) {
                _uiState.update {
                    it.copy(
                        showOnlineRestoredBanner = true,
                        toastMessage = "✅ ইন্টারনেট সচল! ক্যাটালগ রিফ্রেশ হচ্ছে..."
                    )
                }
                viewModelScope.launch { repository.ensureRemoteDefaults() }
                retryLoading()

                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(showOnlineRestoredBanner = false) }
            } else {
                _uiState.update {
                    it.copy(
                        toastMessage = "❌ এখনো ইন্টারনেট সংযোগ পাওয়া যায়নি। অনুগ্রহ করে আপনার ওয়াইফাই বা মোবাইল ডেটা চেক করুন।"
                    )
                }
            }
        }
    }

    private fun loadData() {
        // Make sure first-run remote defaults (rooms + settings) exist.
        // A failure here is non-fatal (streams carry their own fallbacks),
        // but the user gets a toast when online so problems stay visible.
        viewModelScope.launch {
            val result = repository.ensureRemoteDefaults()
            result.onFailure { e ->
                android.util.Log.w("CatalogViewModel", "Remote defaults check failed", e)
                if (_uiState.value.isOnline) {
                    _uiState.update {
                        it.copy(toastMessage = "⚠️ সার্ভারের সাথে সংযোগ করা যায়নি — আবার চেষ্টা করুন।")
                    }
                }
            }
        }

        startContentObservers()
    }

    /**
     * (Re)subscribes the categories + designs streams. Previous collectors
     * are cancelled first so retry never stacks duplicates. Room and settings
     * streams are long-lived with built-in fallbacks and are left running.
     */
    fun retryLoading() {
        categoriesJob?.cancel()
        designsJob?.cancel()
        _uiState.update { it.copy(designsError = null, categoriesError = null) }
        startContentObservers()
    }

    private fun startContentObservers() {
        // Observe Categories
        categoriesJob = viewModelScope.launch {
            try {
                repository.observeCategories().collect { categories ->
                    val resolvedCategories = categories.map { cat ->
                        val displayName = cat.getDisplayName()
                        val slug = if (cat.slug.isNotBlank()) cat.slug else when {
                            cat.id.contains("wallpaper", ignoreCase = true) -> "3d-wallpaper"
                            cat.id.contains("ceiling", ignoreCase = true) -> "3d-ceiling-paper"
                            cat.id.contains("epoxy", ignoreCase = true) -> "3d-epoxy-floor"
                            else -> cat.id
                        }
                        cat.copy(name = displayName, slug = slug)
                    }
                    _uiState.update { it.copy(categories = resolvedCategories, categoriesError = null) }
                }
            } catch (e: Exception) {
                // Cancellation on retry is normal — not an error.
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("CatalogViewModel", "Categories stream failed", e)
                _uiState.update {
                    if (it.categories.isNotEmpty()) {
                        // Cached filters stay; just notify.
                        it.copy(toastMessage = "⚠️ ক্যাটাগরি রিফ্রেশ ব্যর্থ হয়েছে।")
                    } else {
                        it.copy(
                            categoriesError = "ক্যাটাগরি লোড করা যায়নি। ইন্টারনেট চেক করে আবার চেষ্টা করুন।"
                        )
                    }
                }
            }
        }

        // Observe Designs
        designsJob = viewModelScope.launch {
            _uiState.update { if (it.allDesigns.isEmpty()) it.copy(isLoading = true) else it }
            try {
                repository.observeDesigns().collect { designs ->
                    _uiState.update { current ->
                        val newState = current.copy(
                            allDesigns = designs,
                            isLoading = false,
                            designsError = null
                        )
                        applyFiltersAndPaging(newState, resetPage = true)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("CatalogViewModel", "Designs stream failed", e)
                _uiState.update {
                    if (it.allDesigns.isNotEmpty()) {
                        // Cached designs stay visible; just notify.
                        it.copy(
                            isLoading = false,
                            toastMessage = "⚠️ রিফ্রেশ ব্যর্থ হয়েছে — পুরনো ডিজাইন দেখানো হচ্ছে।"
                        )
                    } else {
                        it.copy(
                            isLoading = false,
                            designsError = "ডিজাইন লোড করা যায়নি। ইন্টারনেট চেক করে আবার চেষ্টা করুন।"
                        )
                    }
                }
            }
        }

        // Observe Room Subcategories (admin-managed, bundled fallback)
        viewModelScope.launch {
            repository.observeSubcategories().collect { subcategories ->
                val resolved = subcategories.ifEmpty { DummyCatalogData.subcategories }
                _uiState.update { it.copy(subcategories = resolved) }
            }
        }

        // Observe Contact + Hero settings (admin-managed, safe defaults)
        viewModelScope.launch {
            repository.observeAppSettings().collect { settings ->
                _uiState.update { it.copy(appSettings = settings) }
            }
        }
    }

    fun selectCategory(slug: String) {
        _uiState.update { current ->
            val newState = current.copy(selectedCategorySlug = slug)
            applyFiltersAndPaging(newState, resetPage = true)
        }
    }

    fun selectSubcategory(slug: String) {
        _uiState.update { current ->
            val newState = current.copy(selectedSubcategorySlug = slug)
            applyFiltersAndPaging(newState, resetPage = true)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            val newState = current.copy(searchQuery = query)
            applyFiltersAndPaging(newState, resetPage = true)
        }
    }

    fun clearSearch() {
        updateSearchQuery("")
    }

    /**
     * Infinite scroll pagination: Loads next batch of items
     */
    fun loadNextPage() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMorePages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = current.currentPage + 1
            val nextCount = nextPage * current.pageSize
            val newPagedList = current.filteredDesigns.take(nextCount)
            val hasMore = newPagedList.size < current.filteredDesigns.size

            _uiState.update {
                it.copy(
                    currentPage = nextPage,
                    pagedDesigns = newPagedList,
                    hasMorePages = hasMore,
                    isLoadingMore = false
                )
            }
        }
    }

    /**
     * Manual refresh: re-checks remote defaults and resubscribes the content
     * streams. Streams are real-time, so this is a lightweight refresh —
     * it never fabricates or duplicates content.
     */
    fun syncFirestore() {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(toastMessage = "❌ ইন্টারনেট সংযোগ ছাড়া রিফ্রেশ করা সম্ভব নয়।") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(toastMessage = "ক্যাটালগ রিফ্রেশ হচ্ছে...") }
            val result = repository.ensureRemoteDefaults()
            result
                .onSuccess {
                    retryLoading()
                    _uiState.update { it.copy(toastMessage = "✅ ক্যাটালগ রিফ্রেশ হয়েছে!") }
                }
                .onFailure { e ->
                    android.util.Log.w("CatalogViewModel", "Manual refresh failed", e)
                    retryLoading()
                    _uiState.update {
                        it.copy(toastMessage = "❌ রিফ্রেশ ব্যর্থ হয়েছে — আবার চেষ্টা করুন।")
                    }
                }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun applyFiltersAndPaging(state: CatalogUiState, resetPage: Boolean): CatalogUiState {
        val filtered = state.allDesigns.filter { design ->
            // Category filter
            val matchesCategory = (state.selectedCategorySlug == "all" ||
                    design.categorySlug.equals(state.selectedCategorySlug, ignoreCase = true))

            // Subcategory filter
            val matchesSubcategory = (state.selectedSubcategorySlug == "all" ||
                    design.subcategorySlug.equals(state.selectedSubcategorySlug, ignoreCase = true))

            // Search filter
            val query = state.searchQuery.trim().lowercase()
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                design.name.lowercase().contains(query) ||
                        design.category.lowercase().contains(query) ||
                        design.subcategory.lowercase().contains(query) ||
                        design.categorySlug.lowercase().contains(query) ||
                        design.subcategorySlug.lowercase().contains(query)
            }

            matchesCategory && matchesSubcategory && matchesSearch
        }

        val page = if (resetPage) 1 else state.currentPage
        val takeCount = page * state.pageSize
        val paged = filtered.take(takeCount)
        val hasMore = paged.size < filtered.size

        return state.copy(
            filteredDesigns = filtered,
            pagedDesigns = paged,
            currentPage = page,
            hasMorePages = hasMore,
            isLoadingMore = false
        )
    }
}

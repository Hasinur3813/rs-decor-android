package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dummy.DummyCatalogData
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.Design
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.data.repository.CatalogRepository
import com.example.util.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val searchResults: List<Design> = emptyList(),
    val totalDesignCount: Long = 0,
    val selectedCategorySlug: String = "all", // "all", or an admin category slug
    val selectedSubcategorySlug: String = "all", // "all", or an admin room slug
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = false,
    val currentPage: Int = 1,
    val pageSize: Int = 24,
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
    private var searchJob: Job? = null
    private var subcategoriesJob: Job? = null
    private var appSettingsJob: Job? = null
    private var pageCursor: DocumentSnapshot? = null
    private var pageRequestId = 0L

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
                    // Reconnect the failed observers and reload only the first design page.
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
        loadFirstPage()
    }

    /**
     * Re-subscribes catalog metadata streams and loads only the first design
     * page. Long-lived room/settings collectors are kept instead of duplicated.
     */
    fun retryLoading() {
        categoriesJob?.cancel()
        _uiState.update { it.copy(designsError = null, categoriesError = null) }
        startContentObservers()
        loadFirstPage()
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

        // Observe Room Subcategories (admin-managed, bundled fallback)
        if (subcategoriesJob?.isActive != true) {
            subcategoriesJob = viewModelScope.launch {
                repository.observeSubcategories().collect { subcategories ->
                    val resolved = subcategories.ifEmpty { DummyCatalogData.subcategories }
                    _uiState.update { it.copy(subcategories = resolved) }
                }
            }
        }

        // Observe Contact + Hero settings (admin-managed, safe defaults)
        if (appSettingsJob?.isActive != true) {
            appSettingsJob = viewModelScope.launch {
                repository.observeAppSettings().collect { settings ->
                    _uiState.update { it.copy(appSettings = settings) }
                }
            }
        }
    }

    fun selectCategory(slug: String) {
        selectFilters(categorySlug = slug)
    }

    fun selectSubcategory(slug: String) {
        selectFilters(subcategorySlug = slug)
    }

    fun selectFilters(
        categorySlug: String = _uiState.value.selectedCategorySlug,
        subcategorySlug: String = _uiState.value.selectedSubcategorySlug
    ) {
        val state = _uiState.value
        if (state.selectedCategorySlug == categorySlug &&
            state.selectedSubcategorySlug == subcategorySlug
        ) return
        _uiState.update {
            it.copy(
                selectedCategorySlug = categorySlug,
                selectedSubcategorySlug = subcategorySlug,
                allDesigns = emptyList(),
                filteredDesigns = emptyList(),
                pagedDesigns = emptyList(),
                totalDesignCount = 0
            )
        }
        loadFirstPage(clearResults = true)
    }

    fun searchDesigns(query: String) {
        searchJob?.cancel()
        val tokens = CatalogRepository.searchTokens(query)
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = emptyList(),
                searchError = null,
                isSearching = tokens.isNotEmpty()
            )
        }
        if (tokens.isEmpty()) return
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val results = repository.searchDesigns(tokens)
                if (_uiState.value.searchQuery == query) {
                    _uiState.update {
                        it.copy(searchResults = results, isSearching = false, searchError = null)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("CatalogViewModel", "Design search failed", e)
                if (_uiState.value.searchQuery == query) {
                    val message = if (
                        (e as? FirebaseFirestoreException)?.code ==
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    ) {
                        "সার্চ index তৈরি হয়নি। admin panel থেকে search indexing শেষ করে Firestore indexes deploy করুন। ${e.message.orEmpty()}"
                    } else {
                        "সার্চ করা যায়নি: ${e.message ?: "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"}"
                    }
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchError = message
                        )
                    }
                }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(searchQuery = "", searchResults = emptyList(), searchError = null, isSearching = false)
        }
    }

    private fun loadFirstPage(showRefreshing: Boolean = false, clearResults: Boolean = false) {
        val requestId = ++pageRequestId
        designsJob?.cancel()
        pageCursor = null
        _uiState.update {
            it.copy(
                isLoading = clearResults || it.allDesigns.isEmpty(),
                isLoadingMore = false,
                isRefreshing = showRefreshing,
                hasMorePages = false,
                designsError = null,
                allDesigns = if (clearResults) emptyList() else it.allDesigns,
                filteredDesigns = if (clearResults) emptyList() else it.filteredDesigns,
                pagedDesigns = if (clearResults) emptyList() else it.pagedDesigns,
                totalDesignCount = if (clearResults) 0 else it.totalDesignCount
            )
        }
        designsJob = viewModelScope.launch {
            try {
                val current = _uiState.value
                val page = repository.loadDesignPage(
                    categorySlug = current.selectedCategorySlug,
                    subcategorySlug = current.selectedSubcategorySlug,
                    pageSize = current.pageSize
                )
                if (requestId != pageRequestId) return@launch
                pageCursor = page.cursor
                _uiState.update {
                    it.copy(
                        allDesigns = page.items,
                        filteredDesigns = page.items,
                        pagedDesigns = page.items,
                        currentPage = 1,
                        hasMorePages = page.hasMore,
                        totalDesignCount = page.totalCount ?: 0L,
                        isLoading = false,
                        isRefreshing = false,
                        designsError = null
                    )
                }
                if (showRefreshing) {
                    val settings = repository.refreshAppSettingsFromServer()
                    _uiState.update {
                        it.copy(appSettings = settings, toastMessage = "✅ ক্যাটালগ রিফ্রেশ হয়েছে!")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (requestId != pageRequestId) return@launch
                android.util.Log.w("CatalogViewModel", "Design page load failed", e)
                val message = catalogLoadError(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        designsError = message,
                        toastMessage = message
                    )
                }
            }
        }
    }

    private fun catalogLoadError(error: Exception): String {
        val firestoreError = error as? FirebaseFirestoreException
        return if (firestoreError?.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
            "ডিজাইন ফিল্টারের Firestore index তৈরি হয়নি। rs-decor-admin-web থেকে firebase deploy --only firestore:indexes চালান। ${error.message.orEmpty()}"
        } else {
            "ডিজাইন লোড করা যায়নি: ${error.message ?: "নেটওয়ার্ক বা Firestore সেটিংস পরীক্ষা করুন।"}"
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val cursor = pageCursor ?: return
        if (current.isLoadingMore || !current.hasMorePages) return
        val requestId = pageRequestId

        designsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val page = repository.loadDesignPage(
                    categorySlug = current.selectedCategorySlug,
                    subcategorySlug = current.selectedSubcategorySlug,
                    pageSize = current.pageSize,
                    cursor = cursor,
                    includeTotalCount = false
                )
                if (requestId != pageRequestId) return@launch
                pageCursor = page.cursor ?: cursor
                _uiState.update { state ->
                    val combined = (state.pagedDesigns + page.items).distinctBy { it.id }
                    state.copy(
                        allDesigns = combined,
                        filteredDesigns = combined,
                        pagedDesigns = combined,
                        currentPage = state.currentPage + 1,
                        hasMorePages = page.hasMore,
                        totalDesignCount = page.totalCount ?: state.totalDesignCount,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (requestId != pageRequestId) return@launch
                android.util.Log.w("CatalogViewModel", "Next design page load failed", e)
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        toastMessage = "পরবর্তী ডিজাইনগুলো লোড করা যায়নি। আবার চেষ্টা করুন।"
                    )
                }
            }
        }
    }

    /**
     * Manual refresh fetches only the current first page and app settings.
     */
    fun syncFirestore() {
        if (!_uiState.value.isOnline) {
            _uiState.update { it.copy(toastMessage = "❌ ইন্টারনেট সংযোগ ছাড়া রিফ্রেশ করা সম্ভব নয়।") }
            return
        }
        if (_uiState.value.isRefreshing) return
        loadFirstPage(showRefreshing = true)
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

}

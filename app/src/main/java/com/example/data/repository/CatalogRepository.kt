package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.dummy.DummyCatalogData
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.Design
import com.example.data.model.Subcategory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class DesignPage(
    val items: List<Design>,
    val cursor: DocumentSnapshot?,
    val hasMore: Boolean,
    val totalCount: Long?
)

class CatalogRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        firestoreForDatabase(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        private const val TAG = "CatalogRepository"

        /**
         * Resolves the Firestore instance for a configured database id.
         * Blank or "(default)" means the project's default database, so a
         * fresh Firebase project works with zero code changes.
         */
        fun firestoreForDatabase(databaseId: String): FirebaseFirestore {
            val id = databaseId.trim()
            return if (id.isEmpty() || id == "(default)") FirebaseFirestore.getInstance()
            else FirebaseFirestore.getInstance(id)
        }

        /**
         * Pure mapping for subcategory rows: drops invalid rows (blank
         * slug/name) that must never appear as filter pills, sorts by the
         * admin-controlled `order`, and falls back to bundled English names.
         * Kept pure so it is unit-testable without Firestore.
         */
        fun toRoomItems(rows: List<Subcategory>): List<DummyCatalogData.SubcategoryItem> =
            rows.filter { it.slug.isNotBlank() && it.name.isNotBlank() }
                .sortedBy { it.order }
                .map {
                    DummyCatalogData.SubcategoryItem(
                        slug = it.slug,
                        name = it.name,
                        nameEn = it.nameEn.ifBlank { it.name }
                    )
                }

        fun newestFirst(designs: List<Design>): List<Design> =
            designs.sortedWith(compareByDescending<Design> { it.createdAtMillis }.thenByDescending { it.id })

        fun searchTokens(query: String): List<String> =
            Normalizer.normalize(query.trim(), Normalizer.Form.NFKC)
                .lowercase()
                .split(Regex("[^A-Za-z0-9\\u0980-\\u09FF]+"))
                .filter { it.length >= 2 }
    }

    /**
     * Observe categories collection in real-time from Firestore.
     *
     * Emits exactly what the cloud holds — possibly empty. Errors are logged
     * and rethrown so the ViewModel can render an honest error state with
     * retry instead of fake content.
     */
    fun observeCategories(): Flow<List<Category>> {
        val col = db.collection("categories")
        return col.snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                Log.w(TAG, "Error fetching categories from Firestore", e)
                throw e
            }
    }

    suspend fun loadDesignPage(
        categorySlug: String,
        subcategorySlug: String,
        pageSize: Int,
        cursor: DocumentSnapshot? = null,
        includeTotalCount: Boolean = true
    ): DesignPage {
        var query: Query = db.collection("designs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        if (categorySlug != "all") query = query.whereEqualTo("categorySlug", categorySlug)
        if (subcategorySlug != "all") query = query.whereEqualTo("subcategorySlug", subcategorySlug)
        if (cursor != null) query = query.startAfter(cursor)

        val totalCount = if (includeTotalCount) {
            query.count().get(AggregateSource.SERVER).await().count
        } else {
            null
        }
        val snapshot = query.limit((pageSize + 1).toLong()).get().await()
        val hasMore = snapshot.documents.size > pageSize
        val pageDocuments = snapshot.documents.take(pageSize)
        val items = newestFirst(pageDocuments.mapNotNull { doc ->
            doc.toObject(Design::class.java)?.copy(
                id = doc.id,
                createdAtMillis = createdAtMillis(doc)
            )
        })
        return DesignPage(
            items = items,
            cursor = pageDocuments.lastOrNull(),
            hasMore = hasMore,
            totalCount = totalCount
        )
    }

    suspend fun searchDesigns(tokens: List<String>, limit: Long = 40): List<Design> {
        val searchTokens = tokens.map { it.trim().lowercase() }
            .filter { it.length >= 2 }
            .distinct()
        if (searchTokens.isEmpty()) return emptyList()

        val anchorToken = searchTokens.maxBy { it.length }
        val requiredTokens = searchTokens.toSet()
        val matchingDesigns = mutableListOf<Design>()
        var cursor: DocumentSnapshot? = null
        val pageSize = limit.coerceAtLeast(1)

        do {
            var query: Query = db.collection("designs")
                .whereArrayContains("searchTokens", anchorToken)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            if (cursor != null) query = query.startAfter(cursor)

            val snapshot = query.limit(pageSize).get().await()
            for (doc in snapshot.documents) {
                val indexedTokens = (doc.get("searchTokens") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toSet()
                    .orEmpty()
                if (!indexedTokens.containsAll(requiredTokens)) continue

                doc.toObject(Design::class.java)?.let { design ->
                    matchingDesigns += design.copy(
                        id = doc.id,
                        createdAtMillis = createdAtMillis(doc)
                    )
                }
                if (matchingDesigns.size >= limit) break
            }

            cursor = snapshot.documents.lastOrNull()
            if (matchingDesigns.size >= limit || snapshot.documents.size < pageSize) break
        } while (cursor != null)

        return matchingDesigns
    }

    /**
     * Observe room subcategories in real-time from Firestore.
     *
     * Falls back to the bundled room list when the collection is empty or
     * unreachable, so room filters are always available. Invalid rows
     * (blank slug/name) are skipped rather than shown.
     */
    fun observeSubcategories(): Flow<List<DummyCatalogData.SubcategoryItem>> {
        val col = db.collection("subcategories")
        return col.snapshots()
            .map { snapshot ->
                if (snapshot.isEmpty) {
                    DummyCatalogData.subcategories
                } else {
                    toRoomItems(
                        snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Subcategory::class.java)
                        }
                    ).ifEmpty { DummyCatalogData.subcategories }
                }
            }
            .catch { e ->
                Log.w(TAG, "Error fetching subcategories from Firestore, using local defaults", e)
                emit(DummyCatalogData.subcategories)
            }
    }

    /**
     * Observe contact numbers + hero banner from `settings/app` in real-time.
     *
     * Any failure (missing doc, no network, malformed fields) degrades to
     * [AppSettings] defaults via [AppSettings.sanitized] — the call/WhatsApp
     * buttons and hero banner always have something safe to render.
     */
    fun observeAppSettings(): Flow<AppSettings> {
        val doc = db.collection("settings").document("app")
        return doc.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    AppSettings()
                } else {
                    (snapshot.toObject(AppSettings::class.java) ?: AppSettings()).sanitized()
                }
            }
            .catch { e ->
                Log.w(TAG, "Error fetching app settings from Firestore, using defaults", e)
                emit(AppSettings())
            }
    }

    /** Fetches app settings from the server, bypassing any stale local cache. */
    suspend fun refreshAppSettingsFromServer(): AppSettings {
        val snapshot = db.collection("settings").document("app").get(Source.SERVER).await()
        return if (snapshot.exists()) {
            (snapshot.toObject(AppSettings::class.java) ?: AppSettings()).sanitized()
        } else {
            AppSettings()
        }
    }

    private fun createdAtMillis(doc: DocumentSnapshot): Long {
        val value = doc.get("createdAt")
        return when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is String -> parseCreatedAt(value)
            else -> 0L
        }
    }

    private fun parseCreatedAt(value: String): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(value)?.time
            }.getOrNull()
        } ?: 0L
    }
}

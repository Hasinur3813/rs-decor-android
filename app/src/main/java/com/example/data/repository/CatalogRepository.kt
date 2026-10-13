package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.dummy.DummyCatalogData
import com.example.data.model.Category
import com.example.data.model.Design
import com.example.data.model.AppSettings
import com.example.data.model.Subcategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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

    /**
     * Observe all designs in real-time from Firestore.
     *
     * Emits exactly what the cloud holds — possibly empty. Errors are logged
     * and rethrown so the ViewModel can render an honest error state with
     * retry instead of sample designs.
     */
    fun observeDesigns(): Flow<List<Design>> {
        val col = db.collection("designs")
        return col.snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Design::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                Log.w(TAG, "Error fetching designs from Firestore", e)
                throw e
            }
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

    /**
     * Ensures first-run remote defaults exist: bundled rooms + default app
     * settings. Strictly additive — existing admin-managed documents are
     * NEVER overwritten, so panel edits survive reinstalls and manual syncs.
     * Notably this never creates designs or categories: the catalog shows
     * only real admin-published content.
     */
    suspend fun ensureRemoteDefaults(): Result<Unit> = runCatching {
        // Both are attempted; a failure in either is propagated so callers
        // (sync button, startup check) can report honestly.
        val subResult = seedSubcategories()
        subResult.onFailure { Log.w(TAG, "Subcategory seeding failed", it) }
        val setResult = seedAppSettings()
        setResult.onFailure { Log.w(TAG, "App-settings seeding failed", it) }
        subResult.getOrThrow()
        setResult.getOrThrow()

        Log.i(TAG, "Remote defaults check completed successfully")
    }

    /**
     * Adds bundled rooms that are missing from `subcategories`.
     * Existing documents are left untouched — admin renames/reorders are safe.
     */
    suspend fun seedSubcategories(): Result<Unit> = runCatching {
        val col = db.collection("subcategories")
        val existing = runCatching { col.get().await() }.getOrNull()
        val existingSlugs = existing?.documents
            ?.mapNotNull { it.getString("slug") }
            ?.toSet() ?: emptySet()

        DummyCatalogData.subcategories.forEachIndexed { index, sub ->
            if (existingSlugs.contains(sub.slug)) return@forEachIndexed
            Log.d(TAG, "Seeding subcategory '${sub.slug}' into Firestore...")
            val id = "sub_${sub.slug}"
            val data = hashMapOf(
                "id" to id,
                "name" to sub.name,
                "nameEn" to sub.nameEn,
                "slug" to sub.slug,
                "order" to index,
                "isActive" to true
            )
            col.document(id).set(data).await()
        }
    }

    /**
     * Creates `settings/app` with production defaults when it does not exist.
     * An existing document is NEVER overwritten — admin contact/hero edits win.
     */
    suspend fun seedAppSettings(): Result<Unit> = runCatching {
        val ref = db.collection("settings").document("app")
        val snap = runCatching { ref.get().await() }.getOrNull()
        if (snap != null && snap.exists()) return@runCatching
        Log.d(TAG, "Seeding default app settings into Firestore...")
        val defaults = AppSettings()
        val data = hashMapOf(
            "phoneNumber" to defaults.phoneNumber,
            "phoneDisplay" to defaults.phoneDisplay,
            "whatsappNumber" to defaults.whatsappNumber,
            "heroTitle" to defaults.heroTitle,
            "heroSubtitle" to defaults.heroSubtitle,
            "heroDescription" to defaults.heroDescription,
            "heroImageUrl" to defaults.heroImageUrl,
            "appTitle" to defaults.appTitle,
            "appSubtitle" to defaults.appSubtitle,
            "logoImageUrl" to defaults.logoImageUrl
        )
        ref.set(data).await()
    }
}

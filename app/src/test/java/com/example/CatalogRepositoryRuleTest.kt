package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.CatalogRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun publicShowcase_canWriteAndObserveRealCatalogWithoutAuth() = runBlocking {
        // No auth sign-in needed for public showcase
        val repository = CatalogRepository(firestore)

        // Real admin-published content (payloads satisfy firestore.rules)
        firestore.collection("categories").document("cat_real_wallpaper").set(
            mapOf(
                "id" to "cat_real_wallpaper",
                "name" to "৩ডি ওয়ালপেপার",
                "title" to "৩ডি ওয়ালপেপার",
                "nameEn" to "3D Wallpaper",
                "slug" to "3d-wallpaper",
                "imageUrl" to "https://example.com/cat.jpg"
            )
        ).await()
        firestore.collection("designs").document("des_real_1").set(
            mapOf(
                "id" to "des_real_1",
                "name" to "রয়্যাল গোল্ডেন লোটাস ৩ডি",
                "category" to "৩ডি ওয়ালপেপার",
                "categorySlug" to "3d-wallpaper",
                "subcategory" to "বেডরুম",
                "subcategorySlug" to "bedroom",
                "imageUrl" to "https://example.com/des.jpg"
            )
        ).await()

        // Observe categories
        val categories = repository.observeCategories().first()
        assertTrue(categories.any { it.id == "cat_real_wallpaper" })

        // Observe designs
        val designs = repository.observeDesigns().first()
        val mine = designs.first { it.id == "des_real_1" }
        assertNotNull(mine.name)
        assertNotNull(mine.category)
        assertNotNull(mine.subcategory)

        // Production rule: only real cloud documents may appear — never
        // sample/dummy content (guards against re-adding dummy seeding).
        assertTrue(designs.none { it.id.startsWith("wp_") || it.id.startsWith("cl_") || it.id.startsWith("ep_") })
        assertTrue(categories.none { it.id.startsWith("cat_3d_") })
    }

    @Test
    fun publicShowcase_canSeedAndObserveSubcategoriesWithoutAuth() = runBlocking {
        val repository = CatalogRepository(firestore)

        val result = repository.seedSubcategories()
        assertTrue(result.isSuccess)

        val rooms = repository.observeSubcategories().first()
        assertTrue(rooms.isNotEmpty())
        // Bundled defaults are present (seed is additive, never destructive)
        assertTrue(rooms.any { it.slug == "bedroom" })
        assertTrue(rooms.any { it.slug == "living_room" })

        // A second seed must not duplicate or overwrite admin-managed rows
        val before = rooms.size
        repository.seedSubcategories()
        val after = repository.observeSubcategories().first().size
        assertTrue(after >= before)
    }

    @Test
    fun publicShowcase_canSeedAndObserveAppSettingsWithoutAuth() = runBlocking {
        val repository = CatalogRepository(firestore)

        val result = repository.seedAppSettings()
        assertTrue(result.isSuccess)

        val settings = repository.observeAppSettings().first()
        // Defaults are never blank — safe to render immediately
        assertTrue(settings.phoneNumber.isNotBlank())
        assertTrue(settings.whatsappNumber.isNotBlank())
        assertTrue(settings.heroTitle.isNotBlank())
        assertTrue(settings.whatsappBaseUrl().startsWith("https://wa.me/"))

        // Re-seed must NOT overwrite an admin-customized document
        firestore.collection("settings").document("app").set(
            mapOf(
                "phoneNumber" to "+8801000000000",
                "phoneDisplay" to "০১০০০-০০০০০০",
                "whatsappNumber" to "8801000000000",
                "heroTitle" to "Custom admin title",
                "heroSubtitle" to "Custom subtitle",
                "heroDescription" to "Custom description",
                "heroImageUrl" to "https://example.com/hero.jpg"
            )
        ).await()
        repository.seedAppSettings()
        val kept = repository.observeAppSettings().first()
        assertEquals("+8801000000000", kept.phoneNumber)
        assertEquals("Custom admin title", kept.heroTitle)
    }

    @Test
    fun publicShowcase_subcategoriesRespectAdminOrderField() = runBlocking {
        val repository = CatalogRepository(firestore)

        // Admin adds a room sorted last via a high `order` value
        firestore.collection("subcategories").document("sub_zz_test_last").set(
            mapOf(
                "id" to "sub_zz_test_last",
                "name" to "টেস্ট রুম",
                "nameEn" to "Test Room",
                "slug" to "zz_test_last",
                "order" to 5000,
                "isActive" to true
            )
        ).await()

        val rooms = repository.observeSubcategories().first()
        val idx = rooms.indexOfFirst { it.slug == "zz_test_last" }
        assertTrue(idx >= 0)
        // Bundled rooms use orders 0..6, so the order-5000 room renders last
        assertEquals(rooms.size - 1, idx)
        assertEquals("টেস্ট রুম", rooms[idx].name)
    }
}

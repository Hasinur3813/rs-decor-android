package com.example

import com.example.data.model.Subcategory
import com.example.data.repository.CatalogRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogRepositoryTest {

    @Test
    fun toRoomItems_dropsInvalidRowsAndUsesAdminOrder() {
        val items = CatalogRepository.toRoomItems(
            listOf(
                Subcategory(name = "Living room", slug = "living_room", order = 2),
                Subcategory(name = "", slug = "invalid", order = 0),
                Subcategory(name = "Bedroom", slug = "bedroom", order = 1)
            )
        )

        assertEquals(listOf("bedroom", "living_room"), items.map { it.slug })
    }

    @Test
    fun searchTokens_normalizesAndSplitsBanglaAndEnglishWords() {
        assertEquals(
            listOf("wallpaper", "বেডরুম"),
            CatalogRepository.searchTokens("  Wallpaper, বেডরুম ")
        )
    }
}

package com.example

import com.example.data.model.AppSettings
import com.example.ui.viewmodel.CatalogUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for the admin-synced config model.
 * No Firestore, no Robolectric — every case runs locally.
 */
class AppSettingsTest {

    @Test
    fun defaults_areSafeToRender() {
        val s = AppSettings()
        assertTrue(s.phoneNumber.isNotBlank())
        assertTrue(s.phoneDisplay.isNotBlank())
        assertTrue(s.whatsappNumber.isNotBlank())
        assertTrue(s.heroTitle.isNotBlank())
        assertTrue(s.whatsappBaseUrl().startsWith("https://wa.me/"))
    }

    @Test
    fun sanitized_fillsBlankFieldsOnly() {
        val s = AppSettings(
            phoneNumber = "",
            phoneDisplay = "custom-display",
            whatsappNumber = "",
            heroTitle = "",
            heroSubtitle = "custom-subtitle",
            heroDescription = "",
            heroImageUrl = "https://example.com/hero.jpg",
            appTitle = "",
            appSubtitle = "custom-sub",
            logoImageUrl = ""
        ).sanitized()

        // Blanks fall back to production defaults…
        assertEquals(AppSettings.DEFAULT_PHONE_NUMBER, s.phoneNumber)
        assertEquals(AppSettings.DEFAULT_WHATSAPP_NUMBER, s.whatsappNumber)
        assertEquals(AppSettings.DEFAULT_HERO_TITLE, s.heroTitle)
        assertEquals(AppSettings.DEFAULT_HERO_DESCRIPTION, s.heroDescription)
        assertEquals(AppSettings.DEFAULT_APP_TITLE, s.appTitle)
        // …set values are preserved…
        assertEquals("custom-display", s.phoneDisplay)
        assertEquals("custom-subtitle", s.heroSubtitle)
        assertEquals("custom-sub", s.appSubtitle)
        // …and blank images stay blank (means "use bundled art").
        assertEquals("https://example.com/hero.jpg", s.heroImageUrl)
        assertEquals("", s.logoImageUrl)
    }

    @Test
    fun whatsappBaseUrl_derivesFromAdminNumber() {
        assertEquals("https://wa.me/8801000000000", AppSettings(whatsappNumber = "8801000000000").whatsappBaseUrl())
        // Tolerates formatting the admin might type in the panel
        assertEquals("https://wa.me/8801000000000", AppSettings(whatsappNumber = "+880 1000-000000").whatsappBaseUrl())
        // Garbage degrades to the production default instead of a broken link
        assertEquals(AppSettings.DEFAULT_WHATSAPP_BASE, AppSettings(whatsappNumber = "abc").whatsappBaseUrl())
    }

    @Test
    fun defaultUiState_carriesSafeSettings() {
        val state = CatalogUiState()
        assertTrue(state.appSettings.phoneNumber.isNotBlank())
        assertTrue(state.subcategories.isNotEmpty())
    }

    @Test
    fun toRoomItems_dropsInvalidRowsAndSortsByOrder() {
        val rows = listOf(
            com.example.data.model.Subcategory(id = "a", name = "B Room", nameEn = "", slug = "b_room", order = 2),
            com.example.data.model.Subcategory(id = "b", name = "", nameEn = "No Name", slug = "no_name", order = 0),
            com.example.data.model.Subcategory(id = "c", name = "Broken", nameEn = "Broken", slug = "", order = 0),
            com.example.data.model.Subcategory(id = "d", name = "A Room", nameEn = "A Room", slug = "a_room", order = 1)
        )
        val items = com.example.data.repository.CatalogRepository.toRoomItems(rows)
        // Invalid rows never become filter pills…
        assertEquals(2, items.size)
        // …valid rows sort by admin order, with English-name fallback
        assertEquals("a_room", items[0].slug)
        assertEquals("b_room", items[1].slug)
        assertEquals("B Room", items[1].nameEn)
    }
}

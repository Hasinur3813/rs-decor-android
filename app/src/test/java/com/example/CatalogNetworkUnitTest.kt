package com.example

import com.example.ui.viewmodel.CatalogUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogNetworkUnitTest {

    @Test
    fun defaultState_hasOnlineTrueAndZeroChecking() {
        val state = CatalogUiState()
        assertTrue(state.isOnline)
        assertFalse(state.isCheckingConnection)
        assertFalse(state.showOnlineRestoredBanner)
    }

    @Test
    fun offlineState_updatesCorrectly() {
        val offlineState = CatalogUiState(
            isOnline = false,
            toastMessage = "ইন্টারনেট সংযোগ প্রয়োজন"
        )
        assertFalse(offlineState.isOnline)
        assertNotNull(offlineState.toastMessage)
        assertTrue(offlineState.categories.isEmpty())
    }

    @Test
    fun onlineRestored_updatesBanner() {
        val restoredState = CatalogUiState(
            isOnline = true,
            showOnlineRestoredBanner = true
        )
        assertTrue(restoredState.isOnline)
        assertTrue(restoredState.showOnlineRestoredBanner)
    }

    @Test
    fun defaultState_showsNoFakeContent() {
        val state = CatalogUiState()
        // Production rule: no sample designs or categories — the UI renders
        // only real cloud content (empty until the admin publishes).
        assertTrue(state.categories.isEmpty())
        assertTrue(state.allDesigns.isEmpty())
        assertTrue(state.filteredDesigns.isEmpty())
        assertTrue(state.isLoading)

        // Room filter labels are config (not content) with a bundled fallback.
        assertEquals(7, state.subcategories.size) // all + 6 rooms
        assertTrue(state.subcategories.any { it.slug == "bedroom" })
        assertTrue(state.subcategories.any { it.slug == "living_room" })
        assertTrue(state.subcategories.any { it.slug == "kids_room" })
        assertTrue(state.subcategories.any { it.slug == "office" })
        assertTrue(state.subcategories.any { it.slug == "kitchen" })
        assertTrue(state.subcategories.any { it.slug == "dining_room" })
        assertFalse(state.subcategories.any { it.slug == "any_room" })
    }
}

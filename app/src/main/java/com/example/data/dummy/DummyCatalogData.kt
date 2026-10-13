package com.example.data.dummy

/**
 * Bundled fallback data — rooms only.
 *
 * This app shows REAL Firestore content exclusively: there are no sample
 * designs or categories anywhere. The room list below is used only when the
 * `subcategories` collection is empty or unreachable, so the filter UI never
 * renders blank. First launch seeds these rooms into Firestore via
 * `CatalogRepository.seedSubcategories()` (additive — admin edits win).
 */
object DummyCatalogData {

    // Room filters: "all" plus every supported room.
    val subcategories = listOf(
        SubcategoryItem("all", "সব রুম", "All Rooms"),
        SubcategoryItem("bedroom", "বেডরুম", "Bedroom"),
        SubcategoryItem("living_room", "লিভিং রুম", "Living Room"),
        SubcategoryItem("kids_room", "কিডস রুম", "Kids Room"),
        SubcategoryItem("office", "অফিস", "Office"),
        SubcategoryItem("kitchen", "কিচেন", "Kitchen"),
        SubcategoryItem("dining_room", "ডাইনিং রুম", "Dining Room")
    )

    data class SubcategoryItem(
        val slug: String,
        val name: String,
        val nameEn: String
    )
}

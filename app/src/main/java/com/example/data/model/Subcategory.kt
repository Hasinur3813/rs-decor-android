package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Room subcategory managed from the admin panel (`subcategories` collection).
 * The app falls back to [com.example.data.dummy.DummyCatalogData.subcategories]
 * when the collection is empty or unreachable, so the UI never renders
 * without room filters.
 */
@IgnoreExtraProperties
data class Subcategory(
    val id: String = "",
    val name: String = "",
    val nameEn: String = "",
    val slug: String = "",
    val order: Int = 999
)

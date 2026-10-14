package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Design(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val categorySlug: String = "",
    val subcategory: String = "",
    val subcategorySlug: String = "",
    val imageUrl: String = "",
    val createdAtMillis: Long = 0L
)

package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Category(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val nameEn: String = "",
    val slug: String = "",
    val imageUrl: String = ""
) {
    fun getDisplayName(): String = when {
        name.isNotBlank() -> name
        title.isNotBlank() -> title
        nameEn.isNotBlank() -> nameEn
        slug == "3d-wallpaper" || id.contains("wallpaper", ignoreCase = true) -> "৩ডি ওয়ালপেপার"
        slug == "3d-ceiling-paper" || id.contains("ceiling", ignoreCase = true) -> "৩ডি সিলিং পেপার"
        slug == "3d-epoxy-floor" || id.contains("epoxy", ignoreCase = true) -> "৩ডি ইপক্সি ফ্লোর"
        else -> "৩ডি ডিজাইন"
    }
}

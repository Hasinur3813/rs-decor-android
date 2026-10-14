package com.example.ui.common

fun designThumbnailUrl(url: String): String {
    if (!url.contains("res.cloudinary.com/") || !url.contains("/image/upload/")) return url
    return url.replace(
        "/image/upload/",
        "/image/upload/w_900,c_limit,f_auto,q_auto/"
    )
}

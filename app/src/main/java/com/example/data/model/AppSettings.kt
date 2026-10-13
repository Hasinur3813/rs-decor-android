package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Contact numbers + home hero banner managed from the admin panel
 * (single document `settings/app`).
 *
 * Every property carries the production default, so a missing document,
 * a partially-filled document, or a Firestore outage all degrade to the
 * exact values the app shipped with — never to a blank screen.
 */
@IgnoreExtraProperties
data class AppSettings(
    val phoneNumber: String = DEFAULT_PHONE_NUMBER,
    val phoneDisplay: String = DEFAULT_PHONE_DISPLAY,
    val whatsappNumber: String = DEFAULT_WHATSAPP_NUMBER,
    val heroTitle: String = DEFAULT_HERO_TITLE,
    val heroSubtitle: String = DEFAULT_HERO_SUBTITLE,
    val heroDescription: String = DEFAULT_HERO_DESCRIPTION,
    val heroImageUrl: String = "",
    val appTitle: String = DEFAULT_APP_TITLE,
    val appSubtitle: String = DEFAULT_APP_SUBTITLE,
    val logoImageUrl: String = ""
) {
    /**
     * Per-field fallback: an admin doc that only sets some fields (or stores
     * blank strings) still renders correctly instead of showing empty text.
     */
    fun sanitized(): AppSettings = copy(
        phoneNumber = phoneNumber.ifBlank { DEFAULT_PHONE_NUMBER },
        phoneDisplay = phoneDisplay.ifBlank { DEFAULT_PHONE_DISPLAY },
        whatsappNumber = whatsappNumber.ifBlank { DEFAULT_WHATSAPP_NUMBER },
        heroTitle = heroTitle.ifBlank { DEFAULT_HERO_TITLE },
        heroSubtitle = heroSubtitle.ifBlank { DEFAULT_HERO_SUBTITLE },
        heroDescription = heroDescription.ifBlank { DEFAULT_HERO_DESCRIPTION },
        appTitle = appTitle.ifBlank { DEFAULT_APP_TITLE },
        appSubtitle = appSubtitle.ifBlank { DEFAULT_APP_SUBTITLE }
        // heroImageUrl / logoImageUrl intentionally NOT defaulted:
        // blank means "use bundled art".
    )

    /** `https://wa.me/<digits>` derived from the admin-controlled number. */
    fun whatsappBaseUrl(): String {
        val digits = whatsappNumber.filter { it.isDigit() }
        return if (digits.length >= 8) "https://wa.me/$digits" else DEFAULT_WHATSAPP_BASE
    }

    companion object {
        const val DEFAULT_PHONE_NUMBER = "+8801976600300"
        const val DEFAULT_PHONE_DISPLAY = "০১৯৭৬-৬০০৩০০"
        const val DEFAULT_WHATSAPP_NUMBER = "8801976600300"
        const val DEFAULT_WHATSAPP_BASE = "https://wa.me/8801976600300"
        const val DEFAULT_HERO_TITLE = "আর এস ৩ডি ওয়ালপেপার অ্যান্ড ফ্লোর"
        const val DEFAULT_HERO_SUBTITLE = "৩ডি ওয়ালপেপার • ৩ডি সিলিং পেপার • ৩ডি ইপক্সি ফ্লোর"
        const val DEFAULT_HERO_DESCRIPTION = "সারা বাংলাদেশে হোম ডেলিভারি ও প্রফেশনাল ফিটিং সার্ভিস"
        const val DEFAULT_APP_TITLE = "আর এস ৩ডি ডেকর"
        const val DEFAULT_APP_SUBTITLE = "প্রিমিয়াম ডিজাইন শোকেস"
    }
}

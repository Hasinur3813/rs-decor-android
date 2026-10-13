package com.example.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.model.AppSettings
import java.net.URLEncoder

/**
 * Direct contact entry points (Call + WhatsApp).
 *
 * Numbers come from the admin panel (`settings/app`, surfaced as
 * [AppSettings]); the constants below are offline-safe defaults so the
 * buttons keep working when remote config is unavailable. Blank numbers
 * are rejected with a user-facing message instead of firing a broken intent.
 */
object ContactActions {
    private const val TAG = "ContactActions"

    const val PHONE_NUMBER = AppSettings.DEFAULT_PHONE_NUMBER
    const val PHONE_DISPLAY = AppSettings.DEFAULT_PHONE_DISPLAY
    const val WHATSAPP_BASE = AppSettings.DEFAULT_WHATSAPP_BASE
    const val DEFAULT_WHATSAPP_NUMBER = AppSettings.DEFAULT_WHATSAPP_NUMBER

    fun callNow(context: Context, phoneNumber: String = PHONE_NUMBER) {
        val number = phoneNumber.trim()
        if (number.isEmpty()) {
            Log.w(TAG, "callNow aborted: phone number is blank")
            Toast.makeText(context, "ফোন নম্বর পাওয়া যায়নি। পরে আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "callNow failed for $number", e)
            Toast.makeText(context, "ডায়ালার ওপেন করা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(
        context: Context,
        prefilledMessage: String,
        whatsappNumber: String = DEFAULT_WHATSAPP_NUMBER
    ) {
        val digits = whatsappNumber.filter { it.isDigit() }
        if (digits.length < 8) {
            Log.w(TAG, "openWhatsApp aborted: invalid number '$whatsappNumber'")
            Toast.makeText(context, "হোয়াটসঅ্যাপ নম্বর পাওয়া যায়নি। পরে আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val encodedMessage = URLEncoder.encode(prefilledMessage, "UTF-8")
            val url = "https://wa.me/$digits?text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "openWhatsApp failed", e)
            Toast.makeText(context, "হোয়াটসঅ্যাপ ওপেন করা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.maskan.mobileapp.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Digits and a leading '+' only — required for the wa.me deep link, tel: is lenient enough to skip this. */
private fun sanitizePhoneNumber(phoneNumber: String): String =
    phoneNumber.filterIndexed { index, c -> c.isDigit() || (c == '+' && index == 0) }

fun isWhatsAppInstalled(context: Context): Boolean =
    runCatching { context.packageManager.getPackageInfo("com.whatsapp", 0) }.isSuccess

fun launchPhoneDialer(context: Context, phoneNumber: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
}

fun launchWhatsAppChat(context: Context, phoneNumber: String) {
    val number = sanitizePhoneNumber(phoneNumber)
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")))
}

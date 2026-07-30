package com.maskan.mobileapp.ui.landlord.properties

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Downscaled to max 300px wide, JPEG quality 0.7 — mirrors the iOS upload flow (02-data-models.md). */
fun compressImageForUpload(context: Context, uri: Uri, maxWidth: Int = 300, quality: Int = 70): ByteArray? {
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val original = input.use { BitmapFactory.decodeStream(it) } ?: return null

    val scale = maxWidth.toFloat() / original.width
    val resized = if (scale < 1f) {
        Bitmap.createScaledBitmap(original, maxWidth, (original.height * scale).toInt(), true)
    } else {
        original
    }

    return ByteArrayOutputStream().use { output ->
        resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
        output.toByteArray()
    }
}

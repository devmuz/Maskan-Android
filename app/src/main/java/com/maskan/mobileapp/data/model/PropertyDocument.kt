package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * `properties/{propertyId}/documents` subcollection (feature_documents.md /
 * 02-data-models.md) — metadata for a file uploaded to Firebase Storage and
 * linked to a property.
 */
data class PropertyDocument(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val downloadURL: String = "",
    /** Full Storage path — used for deletion. */
    val storagePath: String = "",
    /** `application/pdf` | `image/jpeg` | `image/png`. */
    val mimeType: String = "",
    /** Bytes after compression. */
    val fileSize: Int = 0,
    @ServerTimestamp
    val uploadedAt: Date? = null,
) {
    @get:Exclude
    val isPdf: Boolean
        get() = mimeType == "application/pdf"

    @get:Exclude
    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    @get:Exclude
    val formattedSize: String
        get() {
            val bytes = fileSize.toDouble()
            return when {
                bytes < 1024 -> "$fileSize B"
                bytes < 1_048_576 -> "%.0f KB".format(bytes / 1024)
                else -> "%.1f MB".format(bytes / 1_048_576)
            }
        }
}

package com.maskan.mobileapp.data.repository

import com.maskan.mobileapp.data.util.StorageEnv
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.maskan.mobileapp.data.model.PropertyDocument
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024
private const val MAX_IMAGE_DIMENSION = 1920
private const val IMAGE_QUALITY = 75

/**
 * `tenants/{tenantId}/documents` subcollection + Storage at
 * `tenants/{landlordId}/{tenantId}/documents/{uuid}_{filename}` — mirrors
 * [DocumentRepository] exactly, just rescoped from property to tenant (and
 * the Storage path needs landlordId since the `tenants/{ownerId}` Storage
 * rule (wildcard-matched) is keyed by landlord, same as the tenant photo-upload path).
 * Not a shared singleton — screens create their own instance and
 * start/stop it per tenantId, same pattern as DocumentRepository.
 */
class TenantDocumentRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {
    private val _documents = MutableStateFlow<List<PropertyDocument>>(emptyList())
    val documents: StateFlow<List<PropertyDocument>> = _documents

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress

    private var listenerJob: Job? = null
    private var currentTenantId: String? = null

    fun startListening(scope: CoroutineScope, tenantId: String) {
        if (tenantId == currentTenantId) return
        currentTenantId = tenantId
        listenerJob?.cancel()
        listenerJob = documentsFlow(tenantId).onEach { _documents.value = it }.launchIn(scope)
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
        currentTenantId = null
        _documents.value = emptyList()
    }

    private fun documentsFlow(tenantId: String) = callbackFlow {
        val registration = documentsCollection(tenantId)
            .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.toObjects(PropertyDocument::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    private fun documentsCollection(tenantId: String) =
        firestore.collection("tenants").document(tenantId).collection("documents")

    /** Uploads a file picked via `ActivityResultContracts.OpenDocument()` or a photo picker (PDF or image). */
    suspend fun upload(context: Context, uri: Uri, tenantId: String, landlordId: String) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: guessMimeType(uri)
        val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment.orEmpty()
        val baseName = displayName.substringBeforeLast('.', displayName)

        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        if (rawBytes.size > MAX_UPLOAD_BYTES) throw DocumentTooLargeException()

        val bytes = if (mimeType.startsWith("image/")) compressImage(rawBytes) else rawBytes
        performUpload(bytes, displayName.ifBlank { "Document" }, baseName.ifBlank { "Document" }, mimeType, tenantId, landlordId)
    }

    private suspend fun performUpload(bytes: ByteArray, filename: String, baseName: String, mimeType: String, tenantId: String, landlordId: String) {
        val path = "tenants/${StorageEnv.folder}/$landlordId/$tenantId/documents/${UUID.randomUUID()}_$filename"
        _isUploading.value = true
        _uploadProgress.value = 0f
        try {
            val ref = storage.reference.child(path)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder().setContentType(mimeType).build()
            ref.putBytes(bytes, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            documentsCollection(tenantId).document().set(
                hashMapOf(
                    "name" to baseName,
                    "downloadURL" to downloadUrl,
                    "storagePath" to path,
                    "mimeType" to mimeType,
                    "fileSize" to bytes.size,
                    "uploadedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        } finally {
            _isUploading.value = false
            _uploadProgress.value = 0f
        }
    }

    suspend fun delete(document: PropertyDocument, tenantId: String) {
        storage.reference.child(document.storagePath).delete().await()
        documentsCollection(tenantId).document(document.id).delete().await()
    }

    private fun compressImage(rawBytes: ByteArray): ByteArray {
        val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
        val longestSide = maxOf(original.width, original.height)
        val scale = if (longestSide > MAX_IMAGE_DIMENSION) MAX_IMAGE_DIMENSION.toFloat() / longestSide else 1f
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        } else {
            original
        }
        return ByteArrayOutputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, output)
            output.toByteArray()
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null) ?: return null
        return cursor.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        }
    }

    private fun guessMimeType(uri: Uri): String = when {
        uri.toString().endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        uri.toString().endsWith(".png", ignoreCase = true) -> "image/png"
        else -> "image/jpeg"
    }
}

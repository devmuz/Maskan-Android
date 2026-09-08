package com.maskan.mobileapp.ui.landlord.properties

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.PropertyDocument
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Full document list/upload screen for a property (feature_documents.md,
 * mirroring iOS's `PropertyDocumentsView`). Reached from Building Detail's
 * Documents row, scoped to that building's representative flat id — the
 * `properties/{propertyId}/documents` subcollection is per-flat, but a
 * building's Documents entry point always uses the same representative flat
 * (05-landlord-properties.md).
 *
 * Unlike iOS, there's no local disk cache + QuickLook preview here — tapping
 * a row opens the file's Storage download URL in an external viewer
 * (browser/PDF app) instead of downloading into an app-private cache first.
 */
@Composable
fun PropertyDocumentsScreen(propertyId: String, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val repository = remember(propertyId) { container.newDocumentRepository() }

    DisposableEffect(propertyId) {
        onDispose { repository.stopListening() }
    }
    LaunchedEffect(propertyId) {
        repository.startListening(scope, propertyId)
    }

    val documents by repository.documents.collectAsStateWithLifecycle()
    val isUploading by repository.isUploading.collectAsStateWithLifecycle()

    var showUploadMenu by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PropertyDocument?>(null) }

    fun upload(uri: android.net.Uri?) {
        if (uri == null) return
        scope.launch {
            try {
                repository.upload(context, uri, propertyId)
            } catch (t: Throwable) {
                errorMessage = t.message ?: "Upload failed. Check your connection and try again."
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> upload(uri) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> upload(uri) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            Text(text = "Documents", style = MaskanType.bodyMedium, color = colors.textPrimary)
            Box {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.gradientStart, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { showUploadMenu = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add document", tint = colors.gradientStart)
                    }
                }
                DropdownMenu(expanded = showUploadMenu, onDismissRequest = { showUploadMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Photo Library") },
                        leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                        onClick = { showUploadMenu = false; photoPicker.launch("image/*") },
                    )
                    DropdownMenuItem(
                        text = { Text("Files") },
                        leadingIcon = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null) },
                        onClick = { showUploadMenu = false; filePicker.launch(arrayOf("application/pdf", "image/*")) },
                    )
                }
            }
        }

        if (documents.isEmpty() && !isUploading) {
            EmptyDocuments()
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding)) {
                MaskanCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    documents.forEachIndexed { index, doc ->
                        DocumentRow(
                            document = doc,
                            onOpen = {
                                runCatching {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(doc.downloadURL)))
                                }
                            },
                            onDeleteRequested = { deleteTarget = doc },
                        )
                        if (index != documents.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(color = colors.border)
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${doc.name}\"?") },
            text = { Text("This will permanently remove the file.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        try {
                            repository.delete(doc, propertyId)
                        } catch (t: Throwable) {
                            errorMessage = t.message ?: "Couldn't delete this file."
                        }
                    }
                }) { Text("Delete", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun EmptyDocuments() {
    val colors = MaskanTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.NoteAdd, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "No documents yet", style = MaskanType.sectionTitle, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap + to upload a PDF or image\nfor this property.",
            style = MaskanType.secondary,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun DocumentRow(document: PropertyDocument, onOpen: () -> Unit, onDeleteRequested: () -> Unit) {
    val colors = MaskanTheme.colors
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = { showMenu = true })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    (if (document.isPdf) Color(0xFFEF4444) else Color(0xFF3B82F6)).copy(alpha = 0.12f),
                    RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (document.isPdf) Icons.Filled.Description else Icons.Filled.Image,
                contentDescription = null,
                tint = if (document.isPdf) Color(0xFFEF4444) else Color(0xFF3B82F6),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = document.name, style = MaskanType.bodyMedium, color = colors.textPrimary, maxLines = 1)
            Row(modifier = Modifier.padding(top = 3.dp)) {
                Text(text = document.formattedSize, style = MaskanType.caption, color = colors.textSecondary)
                document.uploadedAt?.let {
                    Text(text = " · ${dateFormat.format(it)}", style = MaskanType.caption, color = colors.textSecondary)
                }
            }
        }

        Icon(Icons.Filled.OpenInNew, contentDescription = "Open", tint = colors.textTertiary)

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Delete", color = colors.danger) },
                onClick = { showMenu = false; onDeleteRequested() },
            )
        }
    }
}

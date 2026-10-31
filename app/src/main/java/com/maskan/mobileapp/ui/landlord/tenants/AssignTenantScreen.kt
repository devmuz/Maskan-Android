package com.maskan.mobileapp.ui.landlord.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.maskan.mobileapp.data.model.IdentityDocumentType
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.repository.AssignedTenant
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTenantScreen(viewModel: LandlordViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val vacantProperties = remember(properties) { properties.filter { !it.occupied } }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var selectedPropertyId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var moveInDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Optional extra details — none of these are required to add a tenant.
    var identityDocumentType by remember { mutableStateOf<IdentityDocumentType?>(null) }
    var identityDocumentNumber by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var alternateContact by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }
    var propertyMenuExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<AssignedTenant?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable(onClick = onCancel))
            Text(text = if (result == null) "Add Tenant" else "Tenant added", style = MaskanType.cardTitle, color = colors.textPrimary)
            Box(modifier = Modifier.size(56.dp, 1.dp))
        }

        val assigned = result
        if (assigned != null) {
            AssignedCredentialsContent(assigned = assigned, onDone = onDone)
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                if (vacantProperties.isEmpty()) {
                    Text(
                        text = "No vacant properties available. All your properties are currently occupied.",
                        style = MaskanType.body,
                        color = colors.textSecondary,
                    )
                } else {
                    Column {
                        Text(text = "Property", style = MaskanType.fieldLabel, color = colors.textSecondary)
                        Box(modifier = Modifier.padding(top = 6.dp)) {
                            val selectedProperty = vacantProperties.find { it.id == selectedPropertyId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                                    .clickable { propertyMenuExpanded = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = selectedProperty?.let {
                                        if (it.unit.isNotBlank()) "${it.unit} · ${it.displayBuildingName} (${it.propertyIdCode})" else "${it.displayBuildingName} (${it.propertyIdCode})"
                                    } ?: "Choose a vacant property",
                                    style = MaskanType.body,
                                    color = if (selectedProperty != null) colors.textPrimary else colors.textTertiary,
                                )
                            }
                            DropdownMenu(expanded = propertyMenuExpanded, onDismissRequest = { propertyMenuExpanded = false }) {
                                vacantProperties.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(if (p.unit.isNotBlank()) "${p.unit} · ${p.displayBuildingName} (${p.propertyIdCode})" else "${p.displayBuildingName} (${p.propertyIdCode})") },
                                        onClick = { selectedPropertyId = p.id; propertyMenuExpanded = false },
                                    )
                                }
                            }
                        }
                    }

                    AppTextField(value = name, onValueChange = { name = it }, label = "Tenant name", placeholder = "Full name")
                    AppTextField(value = contact, onValueChange = { contact = it }, label = "Contact", placeholder = "Phone number", keyboardType = KeyboardType.Phone)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                            .clickable { showDatePicker = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Move-in date", style = MaskanType.body, color = colors.textPrimary)
                        Text(text = dateFormat.format(moveInDate), style = MaskanType.bodyMedium, color = colors.gradientStart)
                    }

                    OptionalTenantDetailsSection(
                        identityDocumentType = identityDocumentType,
                        onIdentityDocumentTypeChange = { identityDocumentType = it },
                        identityDocumentNumber = identityDocumentNumber,
                        onIdentityDocumentNumberChange = { identityDocumentNumber = it },
                        profession = profession,
                        onProfessionChange = { profession = it },
                        alternateContact = alternateContact,
                        onAlternateContactChange = { alternateContact = it },
                        permanentAddress = permanentAddress,
                        onPermanentAddressChange = { permanentAddress = it },
                    )

                    error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }

                    GradientButton(
                        text = "Add Tenant",
                        isLoading = isSaving,
                        loadingText = "Adding…",
                        enabled = selectedPropertyId != null && name.isNotBlank() && contact.isNotBlank(),
                        modifier = Modifier.padding(bottom = 32.dp),
                        onClick = {
                            val propertyId = selectedPropertyId ?: return@GradientButton
                            isSaving = true
                            error = null
                            scope.launch {
                                try {
                                    result = viewModel.tenantRepository.assignTenant(
                                        landlordId = viewModel.landlordUid,
                                        propertyId = propertyId,
                                        name = name,
                                        contact = contact,
                                        moveInDate = moveInDate,
                                        identityDocumentType = identityDocumentType,
                                        identityDocumentNumber = identityDocumentNumber.trim().ifBlank { null },
                                        permanentAddress = permanentAddress.trim().ifBlank { null },
                                        profession = profession.trim().ifBlank { null },
                                        alternateContact = alternateContact.trim().ifBlank { null },
                                    )
                                } catch (t: Throwable) {
                                    error = t.message ?: "Couldn't assign this tenant."
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = moveInDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { moveInDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }
}

@Composable
private fun AssignedCredentialsContent(assigned: AssignedTenant, onDone: () -> Unit) {
    val colors = MaskanTheme.colors
    val clipboard = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaskanDimens.screenHPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GradientButton(
            text = "Share",
            modifier = Modifier.align(Alignment.End),
            onClick = {
                scope.launch {
                    shareCredentialsImage(context, graphicsLayer)
                }
            },
        )

        // Only this content is captured for the shared image — no header,
        // no Copy/Done buttons (matches iOS's shareCardContent).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    onDrawWithContent {
                        graphicsLayer.record { this@onDrawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
                }
                .background(colors.background)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(64.dp).background(colors.success.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(30.dp))
            }
            Text(text = "Tenant added", style = MaskanType.sectionTitle, color = colors.textPrimary)
            Text(
                text = "Share these credentials with your tenant. The password is shown only once — it can't be viewed again.",
                style = MaskanType.body,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            CredentialCard(label = "Tenant", value = assigned.tenant.name)
            CredentialCard(label = "Property ID", value = assigned.tenant.propertyIdCode)
            CredentialCard(label = "Temporary Password", value = assigned.temporaryPassword)
        }

        GradientButton(
            text = "Copy Both",
            brush = androidx.compose.ui.graphics.SolidColor(colors.fieldBackground),
            contentColor = colors.textPrimary,
            onClick = {
                clipboard.setText(AnnotatedString("Property ID: ${assigned.tenant.propertyIdCode}\nPassword: ${assigned.temporaryPassword}"))
            },
        )
        GradientButton(text = "Done", onClick = onDone)
    }
}

/** Renders [graphicsLayer]'s last-recorded content to a PNG in cache and shares it via ACTION_SEND. */
private suspend fun shareCredentialsImage(
    context: android.content.Context,
    graphicsLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
) {
    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
    val dir = java.io.File(context.cacheDir, "images").apply { mkdirs() }
    val file = java.io.File(dir, "tenant_credentials_${System.currentTimeMillis()}.png")
    java.io.FileOutputStream(file).use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share credentials"))
}

/** Shared by [AssignTenantScreen] and [EditTenantScreen] — none of these fields are required. */
@Composable
internal fun OptionalTenantDetailsSection(
    identityDocumentType: IdentityDocumentType?,
    onIdentityDocumentTypeChange: (IdentityDocumentType?) -> Unit,
    identityDocumentNumber: String,
    onIdentityDocumentNumberChange: (String) -> Unit,
    profession: String,
    onProfessionChange: (String) -> Unit,
    alternateContact: String,
    onAlternateContactChange: (String) -> Unit,
    permanentAddress: String,
    onPermanentAddressChange: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "OPTIONAL DETAILS", style = MaskanType.overline, color = colors.textTertiary)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Identity Document", style = MaskanType.fieldLabel, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IdentityDocumentType.entries.forEach { type ->
                    IdentityTypeChip(
                        label = type.displayName,
                        selected = type == identityDocumentType,
                        onClick = { onIdentityDocumentTypeChange(if (type == identityDocumentType) null else type) },
                    )
                }
            }
            if (identityDocumentType != null) {
                AppTextField(value = identityDocumentNumber, onValueChange = onIdentityDocumentNumberChange, label = "", placeholder = "Document number")
            }
        }

        AppTextField(value = profession, onValueChange = onProfessionChange, label = "Profession", placeholder = "e.g. Software Engineer")
        AppTextField(value = alternateContact, onValueChange = onAlternateContactChange, label = "Alternate Number", placeholder = "Phone number", keyboardType = KeyboardType.Phone)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Permanent Address", style = MaskanType.fieldLabel, color = colors.textPrimary)
            AppTextField(value = permanentAddress, onValueChange = onPermanentAddressChange, label = "", placeholder = "Street, area, city", minLines = 2)
        }
    }
}

@Composable
private fun IdentityTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val brush = if (selected) colors.primaryGradient else androidx.compose.ui.graphics.SolidColor(colors.fieldBackground)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = label, style = MaskanType.bodyMedium, color = if (selected) Color.White else colors.textSecondary)
    }
}

@Composable
private fun CredentialCard(label: String, value: String) {
    val colors = MaskanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
            .padding(16.dp),
    ) {
        Text(text = label, style = MaskanType.fieldLabel, color = colors.textSecondary)
        Text(text = value, style = MaskanType.sectionTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
    }
}

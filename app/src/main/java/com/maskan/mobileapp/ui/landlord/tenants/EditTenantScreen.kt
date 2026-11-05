package com.maskan.mobileapp.ui.landlord.tenants

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.IdentityDocumentType
import com.maskan.mobileapp.data.repository.isBuildingLocked
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
fun EditTenantScreen(viewModel: LandlordViewModel, tenantId: String, onDone: () -> Unit, onCancel: () -> Unit, onUpgradeRequired: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val tenant = tenants.find { it.id == tenantId }
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    // Locked on the free plan: details can't be updated, but the tenant can
    // still be marked as moved out (mirrors iOS EditTenantView `locked`).
    val isLocked = tenant?.let { t -> properties.find { it.id == t.resolvedPropertyId } }
        ?.let { isBuildingLocked(it.groupKey, it.landlordId, viewModel.landlordUid, landlord?.isPro == true, properties) } == true
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var moveInDate by remember { mutableStateOf(Date()) }
    var initialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var identityDocumentType by remember { mutableStateOf<IdentityDocumentType?>(null) }
    var identityDocumentNumber by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var alternateContact by remember { mutableStateOf("") }
    var permanentAddress by remember { mutableStateOf("") }

    var moveOutDate by remember { mutableStateOf(Date()) }
    var showMoveOutDatePicker by remember { mutableStateOf(false) }
    var showMoveOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(tenant?.id) {
        if (tenant != null && !initialized) {
            name = tenant.name
            contact = tenant.contact
            tenant.moveInDate?.let { moveInDate = it }
            identityDocumentType = tenant.identityDocumentType
            identityDocumentNumber = tenant.identityDocumentNumber.orEmpty()
            profession = tenant.profession.orEmpty()
            alternateContact = tenant.alternateContact.orEmpty()
            permanentAddress = tenant.permanentAddress.orEmpty()
            initialized = true
        }
    }

    if (tenant == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Tenant not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable(onClick = onCancel))
            Text(text = "Edit Tenant", style = MaskanType.cardTitle, color = colors.textPrimary)
            Box(modifier = Modifier.width(56.dp))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            if (tenant.isActive) {
                AppTextField(value = name, onValueChange = { name = it }, label = "Tenant name", placeholder = "Full name")
                AppTextField(value = contact, onValueChange = { contact = it }, label = "Contact", placeholder = "Phone number", keyboardType = KeyboardType.Phone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Move-in date", style = MaskanType.body, color = colors.textPrimary)
                    Text(text = dateFormat.format(moveInDate), style = MaskanType.bodyMedium, color = colors.gradientStart)
                }

                if (!isLocked) OptionalTenantDetailsSection(
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

                if (isLocked) {
                    Text(
                        text = "This property is locked on the free plan — tenant details can't be updated. You can still mark the tenant as moved out.",
                        style = MaskanType.secondary,
                        color = colors.textSecondary,
                    )
                }

                error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }

                GradientButton(
                    text = "Save Changes",
                    isLoading = isSaving,
                    loadingText = "Saving…",
                    enabled = name.isNotBlank() && contact.isNotBlank(),
                    onClick = save@{
                        if (isLocked) { onUpgradeRequired(); return@save }
                        isSaving = true
                        error = null
                        scope.launch {
                            try {
                                viewModel.tenantRepository.updateTenant(
                                    tenantId = tenant.id,
                                    name = name,
                                    contact = contact,
                                    moveInDate = moveInDate,
                                    identityDocumentType = identityDocumentType,
                                    identityDocumentNumber = identityDocumentNumber.trim().ifBlank { null },
                                    permanentAddress = permanentAddress.trim().ifBlank { null },
                                    profession = profession.trim().ifBlank { null },
                                    alternateContact = alternateContact.trim().ifBlank { null },
                                )
                                onDone()
                            } catch (t: Throwable) {
                                error = t.message ?: "Couldn't save changes."
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                )

                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 24.dp))

                Text(text = "TENANT LEFT THE PROPERTY?", style = MaskanType.overline, color = colors.textTertiary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                        .clickable { showMoveOutDatePicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Move-out date", style = MaskanType.body, color = colors.textPrimary)
                    Text(text = dateFormat.format(moveOutDate), style = MaskanType.bodyMedium, color = colors.gradientStart)
                }
                GradientButton(
                    text = "Mark as Moved Out",
                    brush = androidx.compose.ui.graphics.SolidColor(colors.warning),
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                    onClick = { showMoveOutConfirm = true },
                )
            } else {
                Text(
                    text = tenant.moveOutDate?.let { "Moved out on ${dateFormat.format(it)}" } ?: "Moved out",
                    style = MaskanType.body,
                    color = colors.warning,
                )
            }
        }
    }

    if (showMoveOutDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = moveOutDate.time)
        DatePickerDialog(
            onDismissRequest = { showMoveOutDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        if (millis >= moveInDate.time) moveOutDate = Date(millis)
                    }
                    showMoveOutDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showMoveOutDatePicker = false }) { Text("Cancel") } },
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }

    if (showMoveOutConfirm) {
        AlertDialog(
            onDismissRequest = { showMoveOutConfirm = false },
            title = { Text("Mark ${tenant.name} as moved out?") },
            text = { Text("Their login will stop working and ${tenant.propertyIdCode} will become available for a new tenant. Their record moves to Old Tenants.") },
            confirmButton = {
                TextButton(onClick = {
                    showMoveOutConfirm = false
                    scope.launch {
                        viewModel.tenantRepository.markMovedOut(tenant, moveOutDate)
                        onDone()
                    }
                }) { Text("Mark Moved Out", color = colors.warning) }
            },
            dismissButton = { TextButton(onClick = { showMoveOutConfirm = false }) { Text("Cancel") } },
        )
    }
}

package com.maskan.mobileapp.ui.landlord.bills

import com.maskan.mobileapp.ui.components.GradientButton
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.UnfoldMore
import com.maskan.mobileapp.data.repository.isBuildingLocked
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.data.model.Frequency
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.repository.NewBillInput
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.data.util.toDate
import com.maskan.mobileapp.data.util.toLocalDate
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.FilterChipBar
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillScreen(viewModel: LandlordViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val allProperties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    // Properties over the free-plan limit are excluded — bills can only be
    // added to properties that aren't locked (as on iOS).
    val properties = remember(allProperties, landlord?.isPro) {
        allProperties.filter { !isBuildingLocked(it.groupKey, it.landlordId, viewModel.landlordUid, landlord?.isPro == true, allProperties) }
    }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var selectedPropertyId by remember { mutableStateOf(properties.firstOrNull()?.id) }
    var propertyMenuExpanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(BillType.RENT) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(type.defaultFrequency) }
    var paidBy by remember { mutableStateOf(PaidBy.TENANT) }
    var amount by remember { mutableStateOf("") }
    var referenceDate by remember { mutableStateOf(LocalDate.now()) }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var showReferenceDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val period = remember(referenceDate, frequency) { PeriodFormatter.periodFor(referenceDate, frequency) }
    val isValid = selectedPropertyId != null && (amount.toDoubleOrNull() ?: 0.0) > 0

    fun propertyTitle(p: com.maskan.mobileapp.data.model.Property) =
        if (p.unit.isBlank()) p.displayBuildingName else "${p.displayBuildingName} – ${p.unit}"

    fun save() {
        val propertyId = selectedPropertyId ?: return
        // Bills must carry the property's primary owner UID, not the caller's —
        // co-owners write bills too, and the security rule requires landlordId to
        // match the property owner (feature-bills.md).
        val ownerId = properties.find { it.id == propertyId }?.landlordId ?: viewModel.landlordUid
        isSaving = true
        error = null
        scope.launch {
            try {
                viewModel.billingRepository.addBill(
                    ownerId,
                    NewBillInput(
                        propertyId = propertyId,
                        type = type,
                        frequency = frequency,
                        paidBy = paidBy,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        referenceDate = referenceDate,
                        dueDate = dueDate.toDate(),
                        notes = notes.ifBlank { null },
                    ),
                )
                onDone()
            } catch (t: Throwable) {
                error = t.message ?: "Something went wrong."
            } finally {
                isSaving = false
            }
        }
    }

    // Same layout as iOS AddBillView: labelled fields, chip pickers, Add Bill at the bottom.
    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Cancel",
                style = MaskanType.body,
                color = colors.gradientStart,
                modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onCancel),
            )
            Text(text = "Add Bill", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LabeledField("Property") {
                val selectedProperty = properties.find { it.id == selectedPropertyId }
                if (properties.isEmpty()) {
                    Text(
                        text = "No properties found. Add a property first.",
                        style = MaskanType.message,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                            .padding(14.dp),
                    )
                } else {
                    Box {
                        PickerField(
                            text = selectedProperty?.let { propertyTitle(it) } ?: "Select property",
                            isPlaceholder = selectedProperty == null,
                            icon = Icons.Filled.UnfoldMore,
                            onClick = { propertyMenuExpanded = true },
                        )
                        DropdownMenu(expanded = propertyMenuExpanded, onDismissRequest = { propertyMenuExpanded = false }) {
                            properties.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(propertyTitle(p)) },
                                    onClick = { selectedPropertyId = p.id; propertyMenuExpanded = false },
                                )
                            }
                        }
                    }
                }
            }

            LabeledField("Bill Type") {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillType.entries.forEach { billType ->
                        PillChip(
                            label = billType.label,
                            icon = billTypeIcon(billType),
                            selected = type == billType,
                            onClick = {
                                type = billType
                                frequency = billType.defaultFrequency
                                if (billType == BillType.RENT) paidBy = PaidBy.TENANT
                            },
                        )
                    }
                }
            }

            LabeledField("Frequency") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Frequency.MONTHLY, Frequency.ANNUAL, Frequency.ONE_TIME).forEach { f ->
                        PillChip(label = f.label, selected = frequency == f, onClick = { frequency = f })
                    }
                }
            }

            LabeledField("Paid By") {
                val paidByLocked = type == BillType.RENT
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaidBy.entries.forEach { option ->
                        PillChip(
                            label = option.label,
                            selected = paidBy == option,
                            enabled = !paidByLocked,
                            onClick = { paidBy = option },
                        )
                    }
                }
                Text(
                    text = if (paidBy == PaidBy.TENANT) "Shown to the tenant, who can mark it paid for you to approve."
                    else "You pay this yourself. It won't be shown to the tenant.",
                    style = MaskanType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                    color = colors.textSecondary,
                )
            }

            AppTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = "Amount",
                placeholder = "0",
                keyboardType = KeyboardType.Decimal,
            )

            LabeledField("Reference Date") {
                PickerField(text = dateFormat.format(referenceDate.toDate()), onClick = { showReferenceDatePicker = true })
                Text(
                    text = "Period: ${PeriodFormatter.displayLabel(period)}",
                    style = MaskanType.caption,
                    color = colors.gradientStart,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            LabeledField("Due Date") {
                PickerField(text = dateFormat.format(dueDate.toDate()), onClick = { showDueDatePicker = true })
            }

            LabeledField("Notes (Optional)") {
                AppTextField(value = notes, onValueChange = { notes = it }, label = "", placeholder = "e.g. Paid govt portal, receipt #...", singleLine = false, minLines = 3)
            }

            GradientButton(
                text = "Add Bill",
                isLoading = isSaving,
                loadingText = "Saving…",
                enabled = isValid,
                onClick = { save() },
            )
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Couldn't save bill") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }

    if (showReferenceDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = referenceDate.toDate().time)
        DatePickerDialog(
            onDismissRequest = { showReferenceDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { referenceDate = Date(it).toLocalDate() }
                    showReferenceDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showReferenceDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showDueDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate.toDate().time)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueDate = Date(it).toLocalDate() }
                    showDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaskanType.fieldLabel, color = MaskanTheme.colors.textSecondary)
        content()
    }
}

/** 52dp tappable field (property menu, dates) — iOS's Menu/DatePicker rows. */
@Composable
private fun PickerField(
    text: String,
    onClick: () -> Unit,
    isPlaceholder: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(MaskanDimens.cornerRadius))
            .background(colors.fieldBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaskanType.body.copy(fontSize = 16.sp),
            color = if (isPlaceholder) colors.textTertiary else colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        icon?.let { Icon(it, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) }
    }
}

/** Capsule chip used by Bill Type / Frequency / Paid By (gradient when selected). */
@Composable
private fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = MaskanTheme.colors
    val contentColor = when {
        selected -> Color.White
        !enabled -> colors.textSecondary
        else -> colors.textPrimary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primaryGradient else androidx.compose.ui.graphics.SolidColor(colors.fieldBackground))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (icon != null) 12.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp)) }
        Text(text = label, style = MaskanType.secondary.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = contentColor)
    }
}

package com.maskan.mobileapp.ui.landlord.bills

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
import com.maskan.mobileapp.ui.components.SegmentedControl
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
    val properties by viewModel.properties.collectAsStateWithLifecycle()
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

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable(onClick = onCancel))
            Text(text = "Add Bill", style = MaskanType.cardTitle, color = colors.textPrimary)
            Text(
                text = "Save",
                style = MaskanType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = if (isValid) colors.gradientStart else colors.textTertiary,
                modifier = Modifier.clickable(enabled = isValid && !isSaving) {
                    val propertyId = selectedPropertyId ?: return@clickable
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.billingRepository.addBill(
                                viewModel.landlordUid,
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
                            error = t.message ?: "Couldn't save this bill."
                        } finally {
                            isSaving = false
                        }
                    }
                },
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Column {
                Text(text = "PROPERTY", style = MaskanType.overline, color = colors.textTertiary)
                val selectedProperty = properties.find { it.id == selectedPropertyId }
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    FieldRow(
                        label = "Property",
                        value = selectedProperty?.let { if (it.unit.isNotBlank()) "${it.displayBuildingName} · ${it.unit}" else it.displayBuildingName } ?: "Select a property",
                        onClick = { propertyMenuExpanded = true },
                    )
                    DropdownMenu(expanded = propertyMenuExpanded, onDismissRequest = { propertyMenuExpanded = false }) {
                        properties.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(if (p.unit.isNotBlank()) "${p.displayBuildingName} · ${p.unit}" else p.displayBuildingName) },
                                onClick = { selectedPropertyId = p.id; propertyMenuExpanded = false },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "BILL DETAILS", style = MaskanType.overline, color = colors.textTertiary)
                Box {
                    FieldRow(
                        label = "Type",
                        value = type.label,
                        icon = billTypeIcon(type),
                        onClick = { typeMenuExpanded = true },
                    )
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        BillType.entries.forEach { billType ->
                            DropdownMenuItem(
                                text = { Text(billType.label) },
                                leadingIcon = { Icon(billTypeIcon(billType), contentDescription = null) },
                                onClick = {
                                    type = billType
                                    frequency = billType.defaultFrequency
                                    if (billType == BillType.RENT) paidBy = PaidBy.TENANT
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                SegmentedControl(
                    options = listOf(Frequency.MONTHLY, Frequency.ANNUAL),
                    selected = frequency,
                    onSelect = { frequency = it },
                    label = { it.label },
                )
                Column {
                    Text(text = "Paid By", style = MaskanType.fieldLabel, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                    val paidByLocked = type == BillType.RENT
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaidBy.entries.forEach { option ->
                            val selected = option == paidBy
                            Box(
                                modifier = Modifier
                                    .background(if (selected) colors.gradientStart else colors.fieldBackground, RoundedCornerShape(50))
                                    .let { if (!paidByLocked) it.clickable { paidBy = option } else it }
                                    .alpha(if (paidByLocked && !selected) 0.4f else 1f)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(text = option.label, style = MaskanType.secondary, color = if (selected) Color.White else colors.textSecondary)
                            }
                        }
                    }
                }
                AppTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = "Amount", keyboardType = KeyboardType.Number)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "PERIOD & DUE DATE", style = MaskanType.overline, color = colors.textTertiary)
                FieldRow(label = "Reference Date", value = dateFormat.format(referenceDate.toDate()), onClick = { showReferenceDatePicker = true })
                FieldRow(label = "Period", value = PeriodFormatter.displayLabel(period), valueColor = colors.gradientStart, onClick = null)
                FieldRow(label = "Due Date", value = dateFormat.format(dueDate.toDate()), onClick = { showDueDatePicker = true })
            }

            Column {
                Text(text = "NOTES (OPTIONAL)", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                AppTextField(value = notes, onValueChange = { notes = it }, label = "", placeholder = "e.g. Paid govt portal, receipt #...", singleLine = false, minLines = 3)
            }

            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
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
private fun FieldRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    valueColor: androidx.compose.ui.graphics.Color? = null,
    onClick: (() -> Unit)?,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaskanType.body, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            icon?.let { Icon(it, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.height(18.dp)) }
            Text(text = value, style = MaskanType.bodyMedium, color = valueColor ?: colors.gradientStart)
        }
    }
}

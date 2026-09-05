package com.maskan.mobileapp.ui.landlord.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.data.model.PaymentMethods
import com.maskan.mobileapp.data.util.toDate
import com.maskan.mobileapp.data.util.toLocalDate
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
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

/**
 * "Record Payment" — reached from the Dashboard quick action (no property picked yet,
 * [fixedPropertyId] null) and from Tenant Detail's hero button (property already known).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(viewModel: LandlordViewModel, fixedPropertyId: String? = null, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var selectedPropertyId by remember { mutableStateOf(fixedPropertyId ?: properties.firstOrNull()?.id) }
    var propertyMenuExpanded by remember { mutableStateOf(false) }
    var billType by remember { mutableStateOf(BillType.RENT) }
    var amount by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(PaymentMethods.billDetailChips.first()) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedProperty = properties.find { it.id == selectedPropertyId }
    LaunchedEffect(selectedProperty?.id, billType) {
        if (billType == BillType.RENT) {
            selectedProperty?.let { amount = it.monthlyRent.toString() }
        } else {
            amount = ""
        }
    }

    val isValid = selectedPropertyId != null && (amount.toDoubleOrNull() ?: 0.0) > 0

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(colors.surface, RoundedCornerShape(50))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart)
            }
            Text(text = "Record Payment", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            if (fixedPropertyId == null) {
                Column {
                    Text(text = "PROPERTY", style = MaskanType.overline, color = colors.textTertiary)
                    Box(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                                .clickable { propertyMenuExpanded = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Property", style = MaskanType.body, color = colors.textPrimary)
                            Text(
                                text = selectedProperty?.let { if (it.unit.isNotBlank()) "${it.displayBuildingName} · ${it.unit}" else it.displayBuildingName }
                                    ?: "Select a property",
                                style = MaskanType.bodyMedium,
                                color = colors.gradientStart,
                            )
                        }
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
            }

            Column {
                Text(text = "Bill Type", style = MaskanType.fieldLabel, color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BillType.entries.forEach { type ->
                        val selected = type == billType
                        Row(
                            modifier = Modifier
                                .background(if (selected) colors.primaryGradient else androidx.compose.ui.graphics.SolidColor(colors.fieldBackground), RoundedCornerShape(50))
                                .clickable { billType = type }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                billTypeIcon(type),
                                contentDescription = null,
                                tint = if (selected) Color.White else colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(text = type.label, style = MaskanType.secondary, color = if (selected) Color.White else colors.textSecondary)
                        }
                    }
                }
            }

            Column {
                Text(text = "Amount", style = MaskanType.fieldLabel, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                AppTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = "", keyboardType = KeyboardType.Number)
            }

            Column {
                Text(text = "Payment Date", style = MaskanType.fieldLabel, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                ) {
                    Text(text = dateFormat.format(paymentDate.toDate()), style = MaskanType.body, color = colors.textPrimary)
                }
            }

            Column {
                Text(text = "Method", style = MaskanType.fieldLabel, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    PaymentMethods.billDetailChips.forEach { chip ->
                        val selected = chip == method
                        Box(
                            modifier = Modifier
                                .background(if (selected) colors.gradientStart else colors.fieldBackground, RoundedCornerShape(50))
                                .clickable { method = chip }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(text = chip, style = MaskanType.secondary, color = if (selected) Color.White else colors.textSecondary)
                        }
                    }
                }
            }

            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }

            Box(modifier = Modifier.padding(bottom = 24.dp))
        }

        GradientButton(
            text = "Record Payment",
            isLoading = isSaving,
            loadingText = "Recording…",
            enabled = isValid,
            brush = colors.accentGradient,
            contentColor = colors.onAccent,
            modifier = Modifier.padding(horizontal = MaskanDimens.screenHPadding).padding(bottom = 24.dp),
            onClick = {
                val propertyId = selectedPropertyId ?: return@GradientButton
                // Must carry the property's primary owner UID, not the caller's — co-owners
                // record payments too, and the security rule requires landlordId to match the
                // property owner (feature-bills.md).
                val ownerId = properties.find { it.id == propertyId }?.landlordId ?: viewModel.landlordUid
                isSaving = true
                error = null
                scope.launch {
                    try {
                        viewModel.billingRepository.recordPaymentForBillType(
                            ownerId,
                            propertyId,
                            billType,
                            amount.toDoubleOrNull() ?: 0.0,
                            paymentDate.toDate(),
                            method,
                            null,
                        )
                        onDone()
                    } catch (t: Throwable) {
                        error = t.message ?: "Couldn't record this payment."
                    } finally {
                        isSaving = false
                    }
                }
            },
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = paymentDate.toDate().time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { paymentDate = Date(it).toLocalDate() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

package com.maskan.mobileapp.ui.landlord.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.model.PaymentMethods
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Edit / Delete for a recorded payment — the Android side of iOS's
 * PaymentActionRow (Bill Detail, Tenant Detail's recent payments, and the
 * tenant's full Payment History). Deleting reverts the bill to pending.
 */

/** Which payment is being edited or deleted on the current screen. */
@Stable
class PaymentActionsState {
    var editing by mutableStateOf<Payment?>(null)
    var deleting by mutableStateOf<Pair<Payment, BillType?>?>(null)
}

@Composable
fun rememberPaymentActionsState() = remember { PaymentActionsState() }

/** ⋮ menu placed at the end of a payment row. */
@Composable
fun PaymentActionMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = MaskanTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Payment actions", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = { expanded = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = colors.danger) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = colors.danger) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}

/** Hosts the Edit Payment and Delete Payment dialogs for [state]. */
@Composable
fun PaymentActionDialogs(state: PaymentActionsState, viewModel: LandlordViewModel, currencyCode: String) {
    state.editing?.let { payment ->
        EditPaymentDialog(
            payment = payment,
            onDismiss = { state.editing = null },
            onSave = { amount, date, method -> viewModel.billingRepository.updatePayment(payment, amount, date, method) },
        )
    }
    state.deleting?.let { (payment, billType) ->
        val scope = rememberCoroutineScope()
        val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
        val colors = MaskanTheme.colors
        val typeName = (billType ?: BillType.RENT).label.lowercase()
        AlertDialog(
            onDismissRequest = { state.deleting = null },
            title = { Text("Delete Payment?") },
            text = {
                Text(
                    "This will remove the $typeName payment of ${AmountFormatter.format(payment.amount, currencyCode)} " +
                        "recorded on ${payment.paidDate?.let { dateFormat.format(it) } ?: "—"} and revert the bill to pending.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.deleting = null
                    scope.launch { runCatching { viewModel.billingRepository.deletePayment(payment) } }
                }) { Text("Delete Payment", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { state.deleting = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPaymentDialog(
    payment: Payment,
    onDismiss: () -> Unit,
    onSave: suspend (amount: Double, paidDate: Date, method: String) -> Unit,
) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    var amountText by remember { mutableStateOf("%.0f".format(Locale.US, payment.amount)) }
    var paidDate by remember { mutableStateOf(payment.paidDate ?: Date()) }
    var method by remember { mutableStateOf(payment.method) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = amountText.trim().toDoubleOrNull()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppTextField(value = amountText, onValueChange = { amountText = it; error = null }, label = "Amount", keyboardType = KeyboardType.Decimal)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Payment Date", style = MaskanType.fieldLabel, color = colors.textSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.fieldBackground, RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                            .padding(14.dp),
                    ) {
                        Text(dateFormat.format(paidDate), style = MaskanType.bodyMedium, color = colors.gradientStart)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Method", style = MaskanType.fieldLabel, color = colors.textSecondary)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethods.billDetailChips.forEach { option ->
                            val selected = option == method
                            Box(
                                modifier = Modifier
                                    .background(if (selected) colors.primaryGradient else androidx.compose.ui.graphics.SolidColor(colors.fieldBackground), RoundedCornerShape(50))
                                    .clickable { method = option }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(option, style = MaskanType.secondary, color = if (selected) Color.White else colors.textPrimary)
                            }
                        }
                    }
                }
                error?.let { Text(it, style = MaskanType.secondary, color = colors.danger) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (amount ?: 0.0) > 0 && !isSaving,
                onClick = {
                    val value = amount ?: return@TextButton
                    isSaving = true
                    scope.launch {
                        try {
                            onSave(value, paidDate, method)
                            onDismiss()
                        } catch (t: Throwable) {
                            error = t.message ?: "Couldn't save changes."
                        } finally {
                            isSaving = false
                        }
                    }
                },
            ) { Text(if (isSaving) "Saving…" else "Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } },
    )

    if (showDatePicker) {
        // No future dates, matching iOS's `in: ...Date()`.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = paidDate.time,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { paidDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

package com.maskan.mobileapp.ui.landlord.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.model.PaymentMethods
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(viewModel: LandlordViewModel, billId: String, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()

    val bill = bills.find { it.id == billId }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var showRecordPayment by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    if (bill == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Bill not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val property = properties.find { it.id == bill.propertyId }
    val billPayments = payments.filter { it.billId == bill.id }.sortedByDescending { it.paidDate }
    val statusColor = when (bill.status) {
        BillStatus.PAID -> colors.success
        BillStatus.OVERDUE -> colors.danger
        BillStatus.PENDING -> colors.warning
        BillStatus.VERIFYING -> colors.warning
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(44.dp).background(colors.gradientStart.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(billTypeIcon(bill.type), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(22.dp))
                    }
                    StatusBadge(text = bill.status.label, color = statusColor)
                }
                Text(text = bill.type.label, style = MaskanType.sectionTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                Text(text = numberFormat.format(bill.amount), style = MaskanType.screenTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DetailRow("Property", property?.let { if (it.unit.isNotBlank()) "${it.displayBuildingName} · ${it.unit}" else it.displayBuildingName } ?: "—")
                    DetailRow("Period", PeriodFormatter.displayLabel(bill.period))
                    bill.frequency?.let { DetailRow("Frequency", it.label) }
                    DetailRow("Paid By", bill.paidBy.label)
                    DetailRow("Due Date", bill.dueDate?.let { dateFormat.format(it) } ?: "—")
                    bill.paidAt?.let { DetailRow("Paid On", dateFormat.format(it)) }
                    bill.notes?.takeIf { it.isNotBlank() }?.let { DetailRow("Notes", it) }
                }
            }

            if (bill.status == BillStatus.VERIFYING) {
                // Tenant submitted proof of payment — Approve/Reject replace the normal
                // record/status buttons entirely (07-landlord-bills.md).
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientButton(
                        text = "Approve Payment",
                        onClick = { scope.launch { viewModel.billingRepository.approvePayment(bill) } },
                    )
                    GradientButton(
                        text = "Reject Payment",
                        brush = androidx.compose.ui.graphics.SolidColor(colors.danger),
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        onClick = { scope.launch { viewModel.billingRepository.rejectPayment(bill) } },
                    )
                }
            } else if (bill.status != BillStatus.PAID) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientButton(text = "Record Payment", onClick = { showRecordPayment = true })
                    when (bill.status) {
                        BillStatus.PENDING -> GradientButton(
                            text = "Mark as Overdue",
                            brush = androidx.compose.ui.graphics.SolidColor(colors.danger),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            onClick = { scope.launch { viewModel.billingRepository.markOverdue(bill.id) } },
                        )
                        BillStatus.OVERDUE -> GradientButton(
                            text = "Mark as Pending",
                            brush = androidx.compose.ui.graphics.SolidColor(colors.gradientStart),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            onClick = { scope.launch { viewModel.billingRepository.markPending(bill.id) } },
                        )
                        BillStatus.PAID, BillStatus.VERIFYING -> Unit
                    }
                }
            }

            if (billPayments.isNotEmpty()) {
                Column {
                    Text(text = "Payment History", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.padding(bottom = 12.dp))
                    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                        billPayments.forEachIndexed { index, payment ->
                            PaymentRow(payment, numberFormat, dateFormat)
                            if (index != billPayments.lastIndex) HorizontalDivider(color = colors.border)
                        }
                    }
                }
            }

            actionError?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }

            Text(
                text = "Delete Bill",
                style = MaskanType.bodyMedium,
                color = colors.danger,
                modifier = Modifier.padding(vertical = 24.dp).clickable { showDeleteConfirm = true },
            )
        }
    }

    if (showRecordPayment) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showRecordPayment = false }, sheetState = sheetState) {
            RecordBillPaymentSheet(
                billAmount = bill.amount,
                typeLabel = bill.type.label,
                periodLabel = PeriodFormatter.displayLabel(bill.period),
                numberFormat = numberFormat,
                onConfirm = { amount, method, notes ->
                    scope.launch {
                        try {
                            viewModel.billingRepository.recordPayment(bill, amount, Date(), method, notes)
                            showRecordPayment = false
                        } catch (t: Throwable) {
                            actionError = t.message
                        }
                    }
                },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this bill?") },
            text = { Text("This permanently removes the bill. Its payment records, if any, are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        viewModel.billingRepository.deleteBill(bill.id)
                        onBack()
                    }
                }) { Text("Delete", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaskanType.body, color = colors.textSecondary)
        Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun PaymentRow(payment: Payment, numberFormat: NumberFormat, dateFormat: SimpleDateFormat) {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(MaskanDimens.cardPadding), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(text = numberFormat.format(payment.amount), style = MaskanType.bodyMedium, color = colors.textPrimary)
            Text(text = payment.method, style = MaskanType.secondary, color = colors.textSecondary)
        }
        Text(text = payment.paidDate?.let { dateFormat.format(it) } ?: "", style = MaskanType.secondary, color = colors.textSecondary)
    }
}

@Composable
private fun RecordBillPaymentSheet(
    billAmount: Double,
    typeLabel: String,
    periodLabel: String,
    numberFormat: NumberFormat,
    onConfirm: (amount: Double, method: String, notes: String?) -> Unit,
) {
    val colors = MaskanTheme.colors
    var amount by remember { mutableStateOf(billAmount.toString()) }
    var method by remember { mutableStateOf(PaymentMethods.billDetailChips.first()) }
    var notes by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Record Payment", style = MaskanType.sectionTitle, color = colors.textPrimary)
        AppTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = "Amount", keyboardType = KeyboardType.Number)

        Column {
            Text(text = "Method", style = MaskanType.fieldLabel, color = colors.textSecondary)
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethods.billDetailChips.forEach { chip ->
                    val selected = chip == method
                    Box(
                        modifier = Modifier
                            .background(if (selected) colors.gradientStart else colors.fieldBackground, RoundedCornerShape(50))
                            .clickable { method = chip }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(text = chip, style = MaskanType.secondary, color = if (selected) androidx.compose.ui.graphics.Color.White else colors.textSecondary)
                    }
                }
            }
        }

        AppTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)", singleLine = false, minLines = 2)

        MaskanCard(modifier = Modifier.fillMaxWidth(), subtle = true) {
            DetailRow("Type", typeLabel)
            DetailRow("Period", periodLabel)
            DetailRow("Bill Amount", numberFormat.format(billAmount))
        }

        GradientButton(
            text = "Record Payment",
            enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
            onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, method, notes.ifBlank { null }) },
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

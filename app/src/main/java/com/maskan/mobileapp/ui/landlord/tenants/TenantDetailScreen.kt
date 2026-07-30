package com.maskan.mobileapp.ui.landlord.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.model.PaymentMethods
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
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
fun TenantDetailScreen(viewModel: LandlordViewModel, tenantId: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val colors = MaskanTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    val tenant = tenants.find { it.id == tenantId }
    val property = tenant?.let { t -> properties.find { it.id == t.propertyId } }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var showDueDayMenu by remember { mutableStateOf(false) }
    var showRecordPayment by remember { mutableStateOf(false) }
    var showAdjustAmount by remember { mutableStateOf(false) }
    var reminderSentMessage by remember { mutableStateOf<String?>(null) }

    if (tenant == null || property == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Tenant not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val currentPeriod = PeriodFormatter.currentMonthRentPeriod()
    val currentBill = bills.find { it.propertyId == property.id && it.type == com.maskan.mobileapp.data.model.BillType.RENT && it.period == currentPeriod }
    val currentAmount = currentBill?.amount ?: property.monthlyRent
    val isPaid = currentBill?.status == BillStatus.PAID
    val propertyPayments = payments.filter { it.propertyId == property.id }.sortedByDescending { it.paidDate }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            if (tenant.isActive) {
                Text(text = "Edit", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable { onEdit(tenant.id) }.padding(end = 12.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = tenant.name, style = MaskanType.sectionTitle, color = colors.textPrimary)
                        val propertyLabel = if (property.unit.isNotBlank()) "${property.unit} · ${property.displayBuildingName}" else property.displayBuildingName
                        Text(text = propertyLabel, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (tenant.contact.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(colors.primaryGradient, CircleShape)
                                .clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${tenant.contact}"))
                                    context.startActivity(intent)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "#", style = MaskanType.secondary, color = colors.gradientStart)
                    Text(text = tenant.propertyIdCode, style = MaskanType.secondary, color = colors.gradientStart)
                }
                tenant.moveInDate?.let {
                    Text(text = "Since ${dateFormat.format(it)}", style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (tenant.isActive) {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    DetailRow("Monthly rent", numberFormat.format(property.monthlyRent))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showDueDayMenu = true }, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Payment due day", style = MaskanType.body, color = colors.textSecondary)
                        Box {
                            Text(text = tenant.rentDueDay?.toString() ?: "Not set", style = MaskanType.bodyMedium, color = colors.gradientStart)
                            DropdownMenu(expanded = showDueDayMenu, onDismissRequest = { showDueDayMenu = false }) {
                                (1..28).forEach { day ->
                                    DropdownMenuItem(text = { Text(day.toString()) }, onClick = {
                                        showDueDayMenu = false
                                        scope.launch { viewModel.tenantRepository.setRentDueDay(tenant.id, day) }
                                    })
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primaryGradient, RoundedCornerShape(MaskanDimens.heroCornerRadius))
                        .padding(MaskanDimens.cardPadding),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = PeriodFormatter.displayLabel(currentPeriod).uppercase(), style = MaskanType.overline, color = Color.White.copy(alpha = 0.85f))
                        Box(modifier = Modifier.background(Color.White, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(text = if (isPaid) "Paid" else "Pending", style = MaskanType.badge, color = if (isPaid) colors.success else colors.warning)
                        }
                    }
                    Text(text = numberFormat.format(currentAmount), style = MaskanType.screenTitle, color = Color.White, modifier = Modifier.padding(top = 8.dp))

                    if (isPaid) {
                        Text(text = "Received this month — nothing pending.", style = MaskanType.body, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 8.dp))
                    } else {
                        tenant.rentDueDay?.let {
                            Text(text = "Due on day $it", style = MaskanType.secondary, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GradientButton(
                                text = "Record Payment",
                                onClick = { showRecordPayment = true },
                                modifier = Modifier.weight(1f),
                            )
                            GradientButton(
                                text = "Remind",
                                brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.18f)),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        val message = "Rent of ${numberFormat.format(currentAmount)} for ${PeriodFormatter.displayLabel(currentPeriod)} is pending (due day ${tenant.rentDueDay ?: "—"}). Please arrange the payment."
                                        viewModel.billingRepository.sendReminder(tenant.id, message)
                                        reminderSentMessage = "Reminder sent"
                                    }
                                },
                            )
                        }
                        Text(
                            text = "Adjust pending amount",
                            style = MaskanType.secondary,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 12.dp).clickable { showAdjustAmount = true },
                        )
                        reminderSentMessage?.let {
                            Text(text = it, style = MaskanType.secondary, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            } else {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tenant.moveOutDate?.let { "Moved out on ${dateFormat.format(it)}" } ?: "Moved out",
                        style = MaskanType.body,
                        color = colors.warning,
                    )
                }
            }

            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(text = "Payment History", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.padding(bottom = 12.dp))
                if (propertyPayments.isEmpty()) {
                    Text(text = "No payments recorded yet.", style = MaskanType.body, color = colors.textSecondary)
                } else {
                    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        propertyPayments.forEachIndexed { index, payment ->
                            PaymentHistoryRow(payment, numberFormat, dateFormat)
                            if (index != propertyPayments.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = colors.border)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordPayment) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showRecordPayment = false }, sheetState = sheetState) {
            RecordRentPaymentSheet(
                suggestedAmount = currentAmount,
                onCancel = { showRecordPayment = false },
                onConfirm = { amount, method, notes ->
                    scope.launch {
                        viewModel.billingRepository.recordRentPayment(viewModel.landlordUid, property.id, amount, Date(), method, notes)
                        showRecordPayment = false
                    }
                },
            )
        }
    }

    if (showAdjustAmount) {
        var amountText by remember { mutableStateOf(currentAmount.toString()) }
        AlertDialog(
            onDismissRequest = { showAdjustAmount = false },
            title = { Text("Adjust pending amount") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    showAdjustAmount = false
                    if (amount != null) {
                        scope.launch {
                            viewModel.billingRepository.setPendingAmount(viewModel.landlordUid, property.id, amount, currentBill?.dueDate ?: Date())
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdjustAmount = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaskanType.body, color = colors.textSecondary)
        Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun PaymentHistoryRow(payment: Payment, numberFormat: NumberFormat, dateFormat: SimpleDateFormat) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(MaskanDimens.cardPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = numberFormat.format(payment.amount), style = MaskanType.bodyMedium, color = colors.textPrimary)
            Text(text = payment.method, style = MaskanType.secondary, color = colors.textSecondary)
        }
        Text(text = payment.paidDate?.let { dateFormat.format(it) } ?: "", style = MaskanType.secondary, color = colors.textSecondary)
    }
}

@Composable
private fun RecordRentPaymentSheet(
    suggestedAmount: Double,
    onCancel: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String?) -> Unit,
) {
    val colors = MaskanTheme.colors
    var amount by remember { mutableStateOf(suggestedAmount.toString()) }
    var method by remember { mutableStateOf(PaymentMethods.tenantRentChips.first()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Record Payment", style = MaskanType.sectionTitle, color = colors.textPrimary)
        com.maskan.mobileapp.ui.components.AppTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = "Amount",
            keyboardType = KeyboardType.Number,
        )
        Column {
            Text(text = "Method", style = MaskanType.fieldLabel, color = colors.textSecondary)
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethods.tenantRentChips.forEach { chip ->
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
        GradientButton(
            text = "Record Payment",
            enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
            onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, method, null) },
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
}

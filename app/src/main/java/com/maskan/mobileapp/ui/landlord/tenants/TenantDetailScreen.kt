package com.maskan.mobileapp.ui.landlord.tenants

import com.maskan.mobileapp.ui.landlord.bills.PaymentActionDialogs
import com.maskan.mobileapp.ui.landlord.bills.PaymentActionMenu
import com.maskan.mobileapp.ui.landlord.bills.rememberPaymentActionsState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.maskan.mobileapp.data.repository.isBuildingLocked
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.Payment
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.ui.components.DetailRow
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeColor
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailScreen(
    viewModel: LandlordViewModel,
    tenantId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onViewAllPayments: (String) -> Unit,
    onRecordPayment: (String) -> Unit,
    onDocumentsClick: (String) -> Unit,
    onUpgradeRequired: () -> Unit,
) {
    val colors = MaskanTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val archivedProperties by viewModel.archivedProperties.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()

    val tenant = tenants.find { it.id == tenantId }
    // Include deleted properties so an old tenant of a since-deleted property still opens (as on iOS).
    val property = tenant?.let { t -> (properties + archivedProperties).find { it.id == t.resolvedPropertyId } }

    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var showDueDayMenu by remember { mutableStateOf(false) }
    var showAdjustAmount by remember { mutableStateOf(false) }
    var showReminderSent by remember { mutableStateOf(false) }
    var showCallChooser by remember { mutableStateOf(false) }
    var showResetPasswordConfirm by remember { mutableStateOf(false) }
    var newTemporaryPassword by remember { mutableStateOf<String?>(null) }
    var resetError by remember { mutableStateOf<String?>(null) }

    if (tenant == null || property == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Tenant not found", style = MaskanType.bodyMedium, color = colors.textPrimary)
                Text(text = "This tenant may have been removed.", style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
        return
    }

    val currentPeriod = PeriodFormatter.currentMonthRentPeriod()
    val currentBill = bills.find { it.propertyId == property.id && it.type == com.maskan.mobileapp.data.model.BillType.RENT && it.period == currentPeriod }
    val currentAmount = currentBill?.amount ?: property.monthlyRent
    val isPaid = currentBill?.status == BillStatus.PAID
    val currencyCode = property.currency ?: "USD"
    val propertyPayments = payments.filter { it.propertyId == property.id }.sortedByDescending { it.paidDate }
    val billsById = remember(bills) { bills.associateBy { it.id } }
    // Over the free-plan limit: viewable, but recording/adjusting/re-dating needs Pro (as on iOS).
    val isLocked = isBuildingLocked(property.groupKey, property.landlordId, viewModel.landlordUid, landlord?.isPro == true, properties)
    val recordPayment = { if (isLocked) onUpgradeRequired() else onRecordPayment(property.id) }
    val paymentActions = rememberPaymentActionsState()

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .shadow(4.dp, CircleShape, ambientColor = colors.shadowColor, spotColor = colors.shadowColor)
                    .background(colors.surface, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
            Text(text = tenant.name, style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.Center))
            if (tenant.isActive) {
                Text(
                    text = "Edit",
                    style = MaskanType.body,
                    color = colors.gradientStart,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable { onEdit(tenant.id) },
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = tenant.name, style = MaskanType.sectionTitle, color = colors.textPrimary)
                        val propertyLabel = if (property.unit.isNotBlank()) "${property.unit} · ${property.displayBuildingName}" else property.displayBuildingName
                        Text(text = propertyLabel, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (tenant.contact.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primaryGradient, CircleShape)
                                .clickable {
                                    if (com.maskan.mobileapp.data.util.isWhatsAppInstalled(context)) {
                                        showCallChooser = true
                                    } else {
                                        com.maskan.mobileapp.data.util.launchPhoneDialer(context, tenant.contact)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "# ${tenant.propertyIdCode}", style = MaskanType.secondary, color = colors.gradientStart, maxLines = 1, softWrap = false)
                    tenant.moveInDate?.let {
                        Text(
                            text = "Since ${dateFormat.format(it)}",
                            style = MaskanType.secondary,
                            color = colors.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            if (hasExtraDetails(tenant)) {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "DETAILS", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    val rows = buildList {
                        tenant.identityDocumentType?.let { add(it.displayName to (tenant.identityDocumentNumber?.takeIf { n -> n.isNotBlank() } ?: "—")) }
                        tenant.profession?.takeIf { it.isNotBlank() }?.let { add("Profession" to it) }
                        tenant.alternateContact?.takeIf { it.isNotBlank() }?.let { add("Alternate Number" to it) }
                        tenant.permanentAddress?.takeIf { it.isNotBlank() }?.let { add("Permanent Address" to it) }
                    }
                    rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        DetailRow(label, value, verticalPadding = 0.dp)
                    }
                }
            }

            MaskanCard(
                modifier = Modifier.fillMaxWidth().clickable { onDocumentsClick(tenant.id) },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = colors.gradientStart)
                        Text(text = "Documents", style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(start = 10.dp))
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                }
            }

            if (tenant.isActive) {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "RENT", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    DetailRow("Monthly rent", AmountFormatter.format(property.monthlyRent, currencyCode), verticalPadding = 0.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { if (isLocked) onUpgradeRequired() else showDueDayMenu = true }, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Payment due day", style = MaskanType.message, color = colors.textSecondary)
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = tenant.rentDueDay?.let { "Day $it" } ?: "Set day", style = MaskanType.bodyMedium, color = if (isLocked) colors.textSecondary else colors.gradientStart)
                                if (isLocked) Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = colors.textSecondary, modifier = Modifier.padding(start = 4.dp).size(14.dp))
                            }
                            DropdownMenu(expanded = showDueDayMenu, onDismissRequest = { showDueDayMenu = false }) {
                                (1..30).forEach { day ->
                                    DropdownMenuItem(text = { Text("Day $day of the month") }, onClick = {
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
                    Text(text = AmountFormatter.format(currentAmount, currencyCode), style = MaskanType.screenTitle, color = Color.White, modifier = Modifier.padding(top = 8.dp))

                    if (isPaid) {
                        Text(text = "Received this month — nothing pending.", style = MaskanType.secondary, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(top = 8.dp))
                        GradientButton(
                            text = "Record Payment",
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White),
                            contentColor = colors.gradientStart,
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(18.dp)) },
                            onClick = recordPayment,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    } else {
                        (tenant.rentDueDay?.let { "Due on day $it of the month" } ?: "Pending this month").let {
                            Text(text = it, style = MaskanType.secondary, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GradientButton(
                                text = "Record Payment",
                                onClick = recordPayment,
                                modifier = Modifier.weight(1f),
                            )
                            GradientButton(
                                text = "Remind",
                                brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.18f)),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        val message = "Rent of ${AmountFormatter.format(currentAmount, currencyCode)} for ${PeriodFormatter.displayLabel(currentPeriod)} is pending${tenant.rentDueDay?.let { " (due day $it)" } ?: ""}. Please arrange the payment."
                                        viewModel.billingRepository.sendReminder(tenant.id, message)
                                        showReminderSent = true
                                    }
                                },
                            )
                        }
                        Text(
                            text = "Adjust pending amount",
                            style = MaskanType.secondary,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 12.dp).clickable { if (isLocked) onUpgradeRequired() else showAdjustAmount = true },
                        )
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

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "RECENT PAYMENTS", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 12.dp))
                    if (propertyPayments.size > 5) {
                        Text(
                            text = "View All",
                            style = MaskanType.secondary.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.gradientStart,
                            modifier = Modifier.clickable { onViewAllPayments(tenant.id) },
                        )
                    }
                }
                if (propertyPayments.isEmpty()) {
                    Text(text = "No payments recorded yet.", style = MaskanType.message, color = colors.textSecondary)
                } else {
                    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        val recentPayments = propertyPayments.take(5)
                        recentPayments.forEachIndexed { index, payment ->
                            val type = billsById[payment.billId]?.type
                            PaymentHistoryRow(
                                payment, type, currencyCode, dateFormat,
                                onEdit = { paymentActions.editing = payment },
                                onDelete = { paymentActions.deleting = payment to type },
                            )
                            if (index != recentPayments.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = colors.border)
                            }
                        }
                    }
                }
            }

            if (tenant.isActive) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(text = "ACCOUNT", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    MaskanCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showResetPasswordConfirm = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(colors.warning, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Text(text = "Reset Login Password", style = MaskanType.bodyMedium, color = colors.textPrimary)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                        }
                    }
                }
            }
        }
    }

    PaymentActionDialogs(paymentActions, viewModel, currencyCode)

    if (showAdjustAmount) {
        var amountText by remember { mutableStateOf(currentAmount.toString()) }
        AlertDialog(
            onDismissRequest = { showAdjustAmount = false },
            title = { Text("Pending amount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Set this month's pending amount (e.g. rent plus carried-over dues).")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    showAdjustAmount = false
                    if (amount != null) {
                        scope.launch {
                            viewModel.billingRepository.setPendingAmount(property.landlordId, property.id, amount, currentBill?.dueDate ?: PeriodFormatter.rentDueDate(tenant.rentDueDay))
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdjustAmount = false }) { Text("Cancel") } },
        )
    }

    if (showResetPasswordConfirm) {
        AlertDialog(
            onDismissRequest = { showResetPasswordConfirm = false },
            title = { Text("Reset Login Password?") },
            text = { Text("A new temporary password will be generated. The current password will stop working immediately.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetPasswordConfirm = false
                    resetError = null
                    scope.launch {
                        try {
                            newTemporaryPassword = viewModel.tenantRepository.regeneratePassword(tenant)
                        } catch (t: Throwable) {
                            resetError = t.message ?: "Couldn't reset this password."
                        }
                    }
                }) { Text("Reset", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showResetPasswordConfirm = false }) { Text("Cancel") } },
        )
    }

    newTemporaryPassword?.let { password ->
        AlertDialog(
            onDismissRequest = { newTemporaryPassword = null },
            title = { Text("Password Reset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Share these updated credentials with ${tenant.name}.",
                        style = MaskanType.body,
                        color = colors.textSecondary,
                    )
                    listOf("Property ID" to tenant.propertyIdCode, "New Password" to password).forEach { (label, value) ->
                        Column(
                            modifier = Modifier.fillMaxWidth().background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius)).padding(16.dp),
                        ) {
                            Text(text = label, style = MaskanType.fieldLabel, color = colors.textSecondary)
                            Text(text = value, style = MaskanType.sectionTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Text(
                        text = "This is the only time the new password is shown. The previous password stopped working immediately.",
                        style = MaskanType.secondary,
                        color = colors.textSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString("Property ID: ${tenant.propertyIdCode}\nPassword: $password"))
                }) { Text("Copy") }
            },
            dismissButton = { TextButton(onClick = { newTemporaryPassword = null }) { Text("Done") } },
        )
    }

    resetError?.let { message ->
        AlertDialog(
            onDismissRequest = { resetError = null },
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { resetError = null }) { Text("OK") } },
        )
    }

    if (showReminderSent) {
        AlertDialog(
            onDismissRequest = { showReminderSent = false },
            title = { Text("Reminder sent") },
            text = { Text("The tenant will see it in their app's notifications.") },
            confirmButton = { TextButton(onClick = { showReminderSent = false }) { Text("OK") } },
        )
    }

    if (showCallChooser) {
        com.maskan.mobileapp.ui.components.CallChooserDialog(
            onDismiss = { showCallChooser = false },
            onCall = { com.maskan.mobileapp.data.util.launchPhoneDialer(context, tenant.contact) },
            onWhatsApp = { com.maskan.mobileapp.data.util.launchWhatsAppChat(context, tenant.contact) },
        )
    }
}

private fun hasExtraDetails(tenant: com.maskan.mobileapp.data.model.Tenant): Boolean =
    tenant.identityDocumentType != null ||
        !tenant.profession.isNullOrBlank() ||
        !tenant.alternateContact.isNullOrBlank() ||
        !tenant.permanentAddress.isNullOrBlank()


@Composable
fun PaymentHistoryRow(
    payment: Payment,
    billType: com.maskan.mobileapp.data.model.BillType?,
    currencyCode: String,
    dateFormat: SimpleDateFormat,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(MaskanDimens.cardPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(billType?.let { billTypeColor(it) } ?: colors.textTertiary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    billType?.let { billTypeIcon(it) } ?: Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(text = billType?.label ?: "Payment", style = MaskanType.bodyMedium, color = colors.textPrimary)
                Text(text = payment.method, style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
            Text(text = AmountFormatter.format(payment.amount, currencyCode), style = MaskanType.bodyMedium, color = colors.textPrimary)
            Text(text = payment.paidDate?.let { dateFormat.format(it) } ?: "", style = MaskanType.secondary, color = colors.textSecondary)
        }
        if (onEdit != null && onDelete != null) {
            PaymentActionMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

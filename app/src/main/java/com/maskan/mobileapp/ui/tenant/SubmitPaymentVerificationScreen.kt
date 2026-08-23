package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.PaymentMethods
import com.maskan.mobileapp.data.util.toDate
import com.maskan.mobileapp.data.util.toLocalDate
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** "I've Paid This Bill" -> this sheet (09-tenant-app.md). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitPaymentVerificationScreen(
    viewModel: TenantSessionViewModel,
    billId: String,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val bill = bills.find { it.id == billId }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    if (bill == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Bill not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    var amount by remember { mutableStateOf(bill.amount.toString()) }
    var paymentDate by remember { mutableStateOf(LocalDate.now()) }
    var method by remember { mutableStateOf("Bank Transfer") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isValid = (amount.toDoubleOrNull() ?: 0.0) > 0

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Cancel", style = MaskanType.body, color = colors.gradientStart, modifier = Modifier.clickable(onClick = onCancel))
            Text(text = "I've Paid This Bill", style = MaskanType.cardTitle, color = colors.textPrimary)
            Text(
                text = if (isSaving) "Saving…" else "Submit",
                style = MaskanType.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (isValid && !isSaving) colors.gradientStart else colors.textTertiary,
                modifier = Modifier.clickable(enabled = isValid && !isSaving) {
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.submitPaymentForVerification(
                                bill = bill,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                paidDate = paymentDate.toDate(),
                                method = method,
                                notes = notes.ifBlank { null },
                            )
                            onDone()
                        } catch (t: Throwable) {
                            error = t.message ?: "Couldn't submit this payment."
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
            AppTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = "Amount Paid",
                keyboardType = KeyboardType.Number,
            )

            Column {
                Text(text = "Payment Date", style = MaskanType.fieldLabel, color = colors.textSecondary)
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .background(colors.fieldBackground, RoundedCornerShape(MaskanDimens.cornerRadius))
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = dateFormat.format(paymentDate.toDate()), style = MaskanType.body, color = colors.textPrimary)
                }
            }

            Column {
                Text(text = "Payment Method", style = MaskanType.fieldLabel, color = colors.textSecondary)
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            AppTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)", placeholder = "e.g. transaction ID", singleLine = false, minLines = 2)

            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = paymentDate.toDate().time,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Date(utcTimeMillis).toLocalDate() <= today
            },
        )
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

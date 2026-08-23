package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Presented as a sheet on iOS, shared by Home and History (09-tenant-app.md).
 * Modeled here as a pushed screen, matching how the rest of this codebase
 * already treats iOS sheets.
 */
@Composable
fun BillDetailScreen(
    viewModel: TenantSessionViewModel,
    billId: String,
    onBack: () -> Unit,
    onPaySubmit: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val bill = bills.find { it.id == billId }

    if (bill == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Bill not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val (statusLabel, statusColor) = tenantBillStatusLabelAndColor(bill)

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
                    StatusBadge(text = statusLabel, color = statusColor)
                }
                Text(text = bill.type.label, style = MaskanType.sectionTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                Text(
                    text = AmountFormatter.format(bill.amount, currencyCode),
                    style = MaskanType.screenTitle,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DetailRow("Period", PeriodFormatter.displayLabel(bill.period))
                    DetailRow("Due Date", bill.dueDate?.let { dateFormat.format(it) } ?: "—")
                    bill.paidAt?.let { DetailRow("Paid On", dateFormat.format(it)) }
                    bill.frequency?.let { DetailRow("Frequency", it.label) }
                    bill.notes?.takeIf { it.isNotBlank() }?.let { DetailRow("Notes", it) }
                }
            }

            when {
                bill.status == BillStatus.VERIFYING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.warning.copy(alpha = 0.12f), RoundedCornerShape(MaskanDimens.cornerRadius))
                            .padding(MaskanDimens.cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = colors.warning)
                        Text(
                            text = "Your payment is being verified by your landlord.",
                            style = MaskanType.bodyMedium,
                            color = colors.textPrimary,
                        )
                    }
                }
                bill.status != BillStatus.PAID -> {
                    GradientButton(text = "I've Paid This Bill", onClick = { onPaySubmit(bill.id) })
                }
            }

            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
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

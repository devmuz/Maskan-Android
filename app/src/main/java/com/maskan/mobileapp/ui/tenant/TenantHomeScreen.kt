package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.util.startOfToday
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.NumberFormat
import java.util.Locale

/**
 * Home tab. Real functionality per 09-tenant-app.md: mirrors the landlord's
 * Tenant Detail "current month" logic so the tenant sees the same pending
 * total the landlord sees for them. The bell icon is present but
 * intentionally not wired (no in-app notifications feed exists yet).
 */
@Composable
fun TenantHomeScreen(viewModel: TenantSessionViewModel) {
    val colors = MaskanTheme.colors
    val tenant by viewModel.tenant.collectAsStateWithLifecycle()
    val property by viewModel.property.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()

    val pendingBills = bills.filter { it.status != BillStatus.PAID }
    val pendingTotal = pendingBills.sumOf { it.amount }
    val today = startOfToday()

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Home", style = MaskanType.screenTitle, color = colors.textPrimary)
            IconButton(onClick = { /* TODO: present in-app notifications feed once designed */ }) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = colors.textPrimary)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryGradient, androidx.compose.foundation.shape.RoundedCornerShape(MaskanDimens.heroCornerRadius))
                    .padding(MaskanDimens.cardPadding),
            ) {
                Text(text = "PENDING DUES", style = MaskanType.overline, color = Color.White.copy(alpha = 0.8f))
                Text(
                    text = if (pendingTotal > 0) NumberFormat.getNumberInstance(Locale.getDefault()).format(pendingTotal) else "—",
                    style = MaskanType.screenTitle,
                    color = Color.White,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = if (tenant == null) "No dues loaded yet" else if (pendingBills.isEmpty()) "You're all caught up" else "${pendingBills.size} bill(s) pending",
                    style = MaskanType.secondary,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (pendingBills.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "All caught up",
                    message = "Your current rent and bills will appear here.",
                )
            } else {
                pendingBills.sortedBy { it.dueDate }.forEach { bill ->
                    MaskanCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = bill.type.label, style = MaskanType.cardTitle, color = colors.textPrimary)
                                Text(text = com.maskan.mobileapp.data.util.PeriodFormatter.displayLabel(bill.period), style = MaskanType.secondary, color = colors.textSecondary)
                            }
                            Text(
                                text = NumberFormat.getNumberInstance(Locale.getDefault()).format(bill.amount),
                                style = MaskanType.cardTitle,
                                color = if (bill.isActuallyOverdue(today)) colors.danger else colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

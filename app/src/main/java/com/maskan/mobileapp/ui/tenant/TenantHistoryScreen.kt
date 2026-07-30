package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** History tab — the tenant's own payments (09-tenant-app.md). */
@Composable
fun TenantHistoryScreen(viewModel: TenantSessionViewModel) {
    val colors = MaskanTheme.colors
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val dateFormat = remember(Locale.getDefault()) { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Text(
            text = "History",
            style = MaskanType.screenTitle,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp),
        )

        if (payments.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Receipt,
                title = "No history yet",
                message = "Your past rent and bill payments will show up here.",
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaskanDimens.screenHPadding),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing),
            ) {
                payments.sortedByDescending { it.paidDate }.forEach { payment ->
                    MaskanCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = NumberFormat.getNumberInstance(Locale.getDefault()).format(payment.amount), style = MaskanType.cardTitle, color = colors.textPrimary)
                                Text(text = payment.method, style = MaskanType.secondary, color = colors.textSecondary)
                            }
                            Text(
                                text = payment.paidDate?.let { dateFormat.format(it) } ?: "",
                                style = MaskanType.secondary,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

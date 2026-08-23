package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import com.maskan.mobileapp.data.util.toLocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Full list of paid bills grouped by calendar month of `paidAt ?? dueDate`
 * (newest month first) — 09-tenant-app.md.
 */
@Composable
fun TenantHistoryScreen(viewModel: TenantSessionViewModel, onBack: () -> Unit, onBillClick: (String) -> Unit) {
    val colors = MaskanTheme.colors
    val paidBills by viewModel.paidBills.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()

    // paidBills is already sorted desc by paidAt ?? dueDate (TenantSessionViewModel).
    val groups = paidBills.groupBy { bill -> (bill.paidAt ?: bill.dueDate)?.toLocalDate()?.let { YearMonth.from(it) } }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }
        Text(
            text = "History",
            style = MaskanType.screenTitle,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = MaskanDimens.screenHPadding, vertical = 8.dp),
        )

        if (paidBills.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Receipt,
                title = "No history yet",
                message = "Your paid bills and rent payments will appear here.",
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                groups.entries.sortedByDescending { it.key }.forEach { (month, monthBills) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = (month?.let { "${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.year}" } ?: "").uppercase(),
                            style = MaskanType.overline,
                            color = colors.textSecondary,
                        )
                        MaskanCard(modifier = Modifier.fillMaxWidth()) {
                            monthBills.forEach { bill ->
                                BillRow(bill = bill, currencyCode = currencyCode, onClick = { onBillClick(bill.id) })
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(bottom = 24.dp)) {}
            }
        }
    }
}

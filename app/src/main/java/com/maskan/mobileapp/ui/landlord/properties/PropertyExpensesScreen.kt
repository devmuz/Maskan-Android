package com.maskan.mobileapp.ui.landlord.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.HorizontalDivider
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
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.ui.components.BillExpenseRow
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeColor
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Full list of landlord-paid expenses for a building — "View All" from
 * Building Detail once it has more than five (iOS PropertyExpensesListView).
 */
@Composable
fun PropertyExpensesScreen(viewModel: LandlordViewModel, propertyId: String, onBack: () -> Unit, onBillClick: (String) -> Unit) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val anchor = properties.find { it.id == propertyId }
    val siblingIds = remember(properties, anchor?.groupKey) {
        properties.filter { it.groupKey == anchor?.groupKey }.map { it.id }.toSet()
    }
    val expenses = remember(bills, siblingIds) {
        bills.filter { it.propertyId in siblingIds && it.paidBy == PaidBy.LANDLORD }.sortedByDescending { it.dueDate?.time ?: 0L }
    }
    val currencyCode = anchor?.currency ?: landlord?.currencyCode ?: "USD"

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Text(text = "Property Expenses", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.Center))
        }

        if (expenses.isEmpty()) {
            EmptyState(icon = Icons.Filled.Inbox, title = "No Expenses", message = "Property expenses will appear here.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaskanDimens.screenHPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    expenses.forEachIndexed { index, bill ->
                        val isPaid = bill.status.raw == "paid"
                        BillExpenseRow(
                            title = bill.type.label,
                            date = bill.dueDate,
                            amount = AmountFormatter.format(bill.amount, currencyCode),
                            statusLabel = if (isPaid) "Paid" else "Pending",
                            statusColor = if (isPaid) colors.success else colors.warning,
                            iconTint = billTypeColor(bill.type),
                            icon = billTypeIcon(bill.type),
                            onClick = { onBillClick(bill.id) },
                        )
                        if (index != expenses.lastIndex) HorizontalDivider(color = colors.border)
                    }
                }
            }
        }
    }
}

package com.maskan.mobileapp.ui.landlord.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.BillType
import com.maskan.mobileapp.data.model.PaidBy
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.data.util.PeriodFormatter
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.SegmentedControl
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.LocalLandlordContentBottomInset
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Locale

private enum class BillFilter(val label: String) { ALL("All"), PENDING("Pending"), OVERDUE("Overdue"), PAID("Paid") }

/**
 * This screen's filter is literally `status == "..."`, unlike the
 * Dashboard's Overdue/Upcoming widgets which recompute overdue from the due
 * date — a late-but-still-"pending" bill won't show under this tab's
 * Overdue filter. Preserved as-is per 07-landlord-bills.md's explicit note
 * that this inconsistency exists in the source app.
 */
@Composable
fun BillsScreen(viewModel: LandlordViewModel, onBillClick: (String) -> Unit, onAddClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val propertiesById = remember(properties) { properties.associateBy { it.id } }
    val fallbackCurrencyCode = landlord?.currencyCode ?: "USD"
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var filter by remember { mutableStateOf(BillFilter.ALL) }
    var typeFilter by remember { mutableStateOf<BillType?>(null) }
    var showTypeMenu by remember { mutableStateOf(false) }
    val filtered = remember(bills, filter, typeFilter) {
        val byStatus = when (filter) {
            BillFilter.ALL -> bills
            BillFilter.PENDING -> bills.filter { it.status == BillStatus.PENDING }
            BillFilter.OVERDUE -> bills.filter { it.status == BillStatus.OVERDUE }
            BillFilter.PAID -> bills.filter { it.status == BillStatus.PAID }
        }
        typeFilter?.let { type -> byStatus.filter { it.type == type } } ?: byStatus
    }
    val grouped = remember(filtered, properties) {
        filtered.groupBy { it.propertyId }
            .toList()
            .sortedBy { (propertyId, _) -> propertiesById[propertyId]?.displayTitle ?: "" }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaskanDimens.screenHPadding,
                end = MaskanDimens.screenHPadding,
                top = 16.dp,
                bottom = LocalLandlordContentBottomInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Bills", style = MaskanType.screenTitle, color = colors.textPrimary)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (typeFilter != null) colors.gradientStart else colors.surface, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = { showTypeMenu = true }) {
                                Icon(
                                    Icons.Filled.FilterAlt,
                                    contentDescription = "Filter by bill type",
                                    tint = if (typeFilter != null) Color.White else colors.gradientStart,
                                )
                            }
                        }
                        DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                            BillType.entries.forEach { billType ->
                                DropdownMenuItem(
                                    text = { Text(billType.label) },
                                    leadingIcon = { Icon(billTypeIcon(billType), contentDescription = null) },
                                    onClick = {
                                        // Tapping the currently-active type again clears the filter (07-landlord-bills.md).
                                        typeFilter = if (typeFilter == billType) null else billType
                                        showTypeMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                SegmentedControl(
                    options = BillFilter.entries,
                    selected = filter,
                    onSelect = { filter = it },
                    label = { it.label },
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Description,
                        title = if (filter == BillFilter.ALL) "No bills yet" else "No ${filter.label.lowercase()} bills",
                        message = if (filter == BillFilter.ALL) {
                            "Tap + to add electricity, water tax, house tax, and other bills."
                        } else {
                            "Bills with ${filter.label.lowercase()} status will appear here."
                        },
                    )
                }
            } else {
                grouped.forEach { (propertyId, propertyBills) ->
                    val property = propertiesById[propertyId]
                    item(key = "header_$propertyId") {
                        Text(
                            text = property?.let { if (it.unit.isNotBlank()) "${it.displayBuildingName} · ${it.unit}" else it.displayBuildingName } ?: "Unknown property",
                            style = MaskanType.cardTitle,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    item(key = "group_$propertyId") {
                        MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                            val sorted = propertyBills.sortedByDescending { it.dueDate }
                            sorted.forEachIndexed { index, bill ->
                                val currencyCode = property?.currency ?: fallbackCurrencyCode
                                BillRow(bill, currencyCode, dateFormat, onClick = { onBillClick(bill.id) })
                                if (index != sorted.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(color = colors.border)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = colors.gradientStart,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .padding(bottom = LocalLandlordContentBottomInset.current),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Bill")
        }
    }
}

@Composable
private fun BillRow(bill: Bill, currencyCode: String, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    // `verifying` shows as "Pending" here — it's an internal state meaning "tenant says
    // they paid, awaiting confirmation," not part of this list's status vocabulary
    // (07-landlord-bills.md's `landlordDisplayStatus`). Bill Detail shows the real status.
    val landlordDisplayLabel = if (bill.status == BillStatus.VERIFYING) BillStatus.PENDING.label else bill.status.label
    val statusColor = when (bill.status) {
        BillStatus.PAID -> colors.success
        BillStatus.OVERDUE -> colors.danger
        BillStatus.PENDING, BillStatus.VERIFYING -> colors.warning
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(MaskanDimens.cardPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(colors.gradientStart.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(billTypeIcon(bill.type), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(18.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = bill.type.label, style = MaskanType.bodyMedium, color = colors.textPrimary)
                    if (bill.paidBy == PaidBy.LANDLORD) {
                        StatusBadge(text = "Expense", color = colors.gradientStart)
                    }
                }
                Text(
                    text = "${PeriodFormatter.displayLabel(bill.period)} · Due ${bill.dueDate?.let { dateFormat.format(it) } ?: "—"}",
                    style = MaskanType.secondary,
                    color = colors.textSecondary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = AmountFormatter.format(bill.amount, currencyCode), style = MaskanType.bodyMedium, color = colors.textPrimary)
            StatusBadge(text = landlordDisplayLabel, color = statusColor, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

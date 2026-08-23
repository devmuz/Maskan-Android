package com.maskan.mobileapp.ui.landlord.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.ServiceRequestStatus
import com.maskan.mobileapp.ui.components.ChartLegend
import com.maskan.mobileapp.ui.components.GroupedBarChart
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatCardData
import com.maskan.mobileapp.ui.components.StatCardGrid
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LandlordViewModel,
    onAddTenant: () -> Unit,
    onAddBill: () -> Unit,
    onRecordPayment: () -> Unit,
    onOpenRequests: () -> Unit,
) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val pendingRequestCount = remember(requests) { requests.count { it.status == ServiceRequestStatus.PENDING } }

    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val overdue = remember(bills) { overdueBills(bills, today) }
    val upcoming = remember(bills) { upcomingBills(bills, today) }
    val chartBuckets = remember(bills, payments) { chartBuckets(bills, payments) }
    val propertiesById = remember(properties) { properties.associateBy { it.id } }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    val statCards = listOf(
        StatCardData(Icons.Filled.Apartment, properties.size.toString(), "Properties", colors.gradientStart),
        StatCardData(Icons.Filled.People, tenants.count { it.isActive }.toString(), "Tenants", colors.success),
        StatCardData(Icons.Filled.MeetingRoom, properties.count { !it.occupied }.toString(), "Vacant", colors.warning),
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.refreshAll()
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Dashboard", style = MaskanType.screenTitle, color = colors.textPrimary)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(colors.surface, CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = onOpenRequests) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Service Requests", tint = colors.gradientStart)
                            }
                        }
                        if (pendingRequestCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 0.dp)
                                    .size(18.dp)
                                    .background(colors.danger, RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (pendingRequestCount > 99) "99" else pendingRequestCount.toString(),
                                    style = MaskanType.caption.copy(fontSize = 9.sp),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primaryGradient, RoundedCornerShape(MaskanDimens.cornerRadius))
                        .padding(MaskanDimens.cardPadding),
                ) {
                    Text(text = "Everything, at a glance", style = MaskanType.cardTitle.copy(fontSize = 22.sp), color = Color.White)
                    Text(
                        text = "Occupancy, dues, and activity across every property you manage.",
                        style = MaskanType.secondary,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing), modifier = Modifier.fillMaxWidth()) {
                    QuickActionButton("Add Tenant", Modifier.weight(1f), onAddTenant)
                    QuickActionButton("Add Bill", Modifier.weight(1f), onAddBill)
                    QuickActionButton("Record Payment", Modifier.weight(1f), onRecordPayment)
                }
            }

            item {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Collected vs. Expense", style = MaskanType.cardTitle, color = colors.textPrimary)
                    Text(
                        text = "Last 6 months, across your whole portfolio",
                        style = MaskanType.secondary,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                    )
                    if (chartBuckets.all { it.collected == 0.0 && it.expense == 0.0 }) {
                        Text(text = "No rent or bills recorded yet.", style = MaskanType.body, color = colors.textSecondary)
                    } else {
                        GroupedBarChart(
                            labels = chartBuckets.map { it.label },
                            seriesA = chartBuckets.map { it.collected },
                            seriesB = chartBuckets.map { it.expense },
                            colorA = colors.gold,
                            colorB = colors.gradientStart,
                        )
                        ChartLegend(
                            entries = listOf("Collected" to colors.gold, "Expense" to colors.gradientStart),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            item { StatCardGrid(statCards) }

            if (overdue.isNotEmpty()) {
                item {
                    BillListCard(
                        title = "Overdue",
                        subtitle = "Needs immediate attention",
                        icon = Icons.Filled.Warning,
                        iconTint = colors.danger,
                        accentColor = colors.danger,
                        bills = overdue,
                        propertiesById = propertiesById,
                        today = today,
                        moreLabel = "more overdue",
                        numberFormat = numberFormat,
                    )
                }
            }

            item {
                BillListCard(
                    title = "Upcoming Bills",
                    subtitle = "Due in the next 7 days",
                    icon = Icons.Filled.CalendarMonth,
                    iconTint = colors.gradientStart,
                    accentColor = null,
                    bills = upcoming,
                    propertiesById = propertiesById,
                    today = today,
                    moreLabel = "more due soon",
                    numberFormat = numberFormat,
                    emptyMessage = "Nothing due in the next 7 days",
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaskanType.caption, color = colors.textPrimary)
    }
}

@Composable
private fun BillListCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    accentColor: Color?,
    bills: List<Bill>,
    propertiesById: Map<String, Property>,
    today: LocalDate,
    moreLabel: String,
    numberFormat: NumberFormat,
    emptyMessage: String? = null,
) {
    val colors = MaskanTheme.colors
    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (accentColor != null) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor, RoundedCornerShape(2.dp)))
            }
            Column(modifier = Modifier.weight(1f).padding(MaskanDimens.cardPadding)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = title, style = MaskanType.cardTitle, color = colors.textPrimary)
                        Text(text = subtitle, style = MaskanType.secondary, color = colors.textSecondary)
                    }
                    Box(
                        modifier = Modifier.size(40.dp).background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }

                if (bills.isEmpty() && emptyMessage != null) {
                    Text(text = emptyMessage, style = MaskanType.body, color = colors.textSecondary, modifier = Modifier.padding(top = 16.dp))
                } else {
                    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        bills.take(4).forEach { bill ->
                            val property = propertiesById[bill.propertyId]
                            DashboardBillRow(bill, property, today, numberFormat)
                        }
                        val remaining = bills.size - 4
                        if (remaining > 0) {
                            Text(text = "+$remaining $moreLabel", style = MaskanType.secondary, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBillRow(bill: Bill, property: Property?, today: LocalDate, numberFormat: NumberFormat) {
    val colors = MaskanTheme.colors
    val statusLabel = dueStatusLabel(bill, today)
    val statusColor = if (statusLabel == "Overdue") colors.danger else colors.warning
    val propertyName = property?.let { propertyDisplayName(it.buildingName, it.name, it.unit) } ?: ""

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(32.dp).background(colors.gradientStart.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(billTypeIcon(bill.type), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(text = bill.type.label, style = MaskanType.bodyMedium, color = colors.textPrimary)
                Text(text = propertyName, style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = numberFormat.format(bill.amount), style = MaskanType.bodyMedium, color = colors.textPrimary)
            Text(text = statusLabel, style = MaskanType.caption, color = statusColor)
        }
    }
}

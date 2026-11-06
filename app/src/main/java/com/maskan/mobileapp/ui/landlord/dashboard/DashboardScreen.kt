package com.maskan.mobileapp.ui.landlord.dashboard

import androidx.compose.foundation.background
import com.maskan.mobileapp.ui.components.isExpandedWidth
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.ui.components.ChartLegend
import com.maskan.mobileapp.ui.components.GroupedBarChart
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatCardData
import com.maskan.mobileapp.ui.components.StatCardGrid
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.LocalLandlordContentBottomInset
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LandlordViewModel,
    onAddTenant: () -> Unit,
    onAddBill: () -> Unit,
    onRecordPayment: () -> Unit,
    onOpenRequests: () -> Unit,
    onBillClick: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val pendingRequestCount = remember(requests) { requests.count { it.status == ServiceRequestStatus.PENDING } }

    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val overdue = remember(bills) { overdueBills(bills, today) }
    val upcoming = remember(bills) { upcomingBills(bills, today) }
    // Tablets in landscape get the iPad layout and a full year in the chart.
    val expanded = isExpandedWidth()
    val chartMonths = if (expanded) 12 else 6
    val chartBuckets = remember(bills, payments, chartMonths) { chartBuckets(bills, payments, monthCount = chartMonths) }
    val propertiesById = remember(properties) { properties.associateBy { it.id } }
    val fallbackCurrencyCode = landlord?.preferredCurrency ?: "USD"

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
        modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaskanDimens.screenHPadding,
                end = MaskanDimens.screenHPadding,
                top = 16.dp,
                bottom = LocalLandlordContentBottomInset.current,
            ),
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

            if (expanded) {
                // Top: hero beside the three stats (equal height).
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        WelcomeCard(large = true, modifier = Modifier.weight(1f).fillMaxHeight())
                        statCards.forEach { card ->
                            TallStatCard(card, modifier = Modifier.width(168.dp).fillMaxHeight())
                        }
                    }
                }
                // Middle: a large chart beside what needs attention.
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        RevenueChartCard(chartBuckets, chartMonths, large = true, modifier = Modifier.weight(1f))
                        Column(modifier = Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            if (overdue.isNotEmpty()) {
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
                                    fallbackCurrencyCode = fallbackCurrencyCode,
                                    onBillClick = onBillClick,
                                )
                            }
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
                                fallbackCurrencyCode = fallbackCurrencyCode,
                                onBillClick = onBillClick,
                                emptyMessage = "Nothing due in the next 7 days.",
                            )
                        }
                    }
                }
                // Bottom: large quick actions.
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickActionButton("Add Tenant", Modifier.weight(1f), Icons.Filled.PersonAddAlt1, large = true, onClick = onAddTenant)
                        QuickActionButton("Add Bill", Modifier.weight(1f), Icons.Filled.NoteAdd, large = true, onClick = onAddBill)
                        QuickActionButton("Record Payment", Modifier.weight(1f), Icons.Filled.CreditCard, large = true, onClick = onRecordPayment)
                    }
                }
            } else {
                item { WelcomeCard(large = false, modifier = Modifier.fillMaxWidth()) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing), modifier = Modifier.fillMaxWidth()) {
                        QuickActionButton("Add Tenant", Modifier.weight(1f), onClick = onAddTenant)
                        QuickActionButton("Add Bill", Modifier.weight(1f), onClick = onAddBill)
                        QuickActionButton("Record Payment", Modifier.weight(1f), onClick = onRecordPayment)
                    }
                }

                item { RevenueChartCard(chartBuckets, chartMonths, large = false, modifier = Modifier.fillMaxWidth()) }

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
                            fallbackCurrencyCode = fallbackCurrencyCode,
                            onBillClick = onBillClick,
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
                        fallbackCurrencyCode = fallbackCurrencyCode,
                        onBillClick = onBillClick,
                        emptyMessage = "Nothing due in the next 7 days.",
                    )
                }
            }
        }
    }
}

/** Tablet ([large]): taller, with the title at the bottom in larger type. */
@Composable
private fun WelcomeCard(large: Boolean, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    Column(
        modifier = modifier
            .then(if (large) Modifier.heightIn(min = 220.dp) else Modifier)
            .background(colors.primaryGradient, RoundedCornerShape(if (large) MaskanDimens.heroCornerRadius else MaskanDimens.cornerRadius))
            .padding(if (large) 30.dp else MaskanDimens.cardPadding),
        verticalArrangement = if (large) Arrangement.Bottom else Arrangement.Top,
    ) {
        Text(
            text = "Everything, at a glance",
            style = if (large) MaskanType.cardTitle.copy(fontSize = 32.sp, lineHeight = 38.sp) else MaskanType.cardTitle.copy(fontSize = 20.sp),
            color = Color.White,
        )
        Text(
            text = "Occupancy, dues, and activity across every property you manage.",
            style = if (large) MaskanType.body.copy(fontSize = 17.sp, lineHeight = 24.sp) else MaskanType.secondary,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun RevenueChartCard(buckets: List<MonthBucket>, months: Int, large: Boolean, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    MaskanCard(modifier = modifier.then(if (large) Modifier.heightIn(min = 380.dp) else Modifier)) {
        Text(text = "Collected vs. Expense", style = MaskanType.cardTitle, color = colors.textPrimary)
        Text(
            text = "Last $months months, across your whole portfolio",
            style = MaskanType.secondary,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
        )
        if (buckets.all { it.collected == 0.0 && it.expense == 0.0 }) {
            if (large) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = colors.textSecondary.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                    Text(text = "No rent or bills recorded yet.", style = MaskanType.body, color = colors.textSecondary, modifier = Modifier.padding(top = 12.dp))
                }
            } else {
                Text(text = "No rent or bills recorded yet.", style = MaskanType.message, color = colors.textSecondary)
            }
        } else {
            GroupedBarChart(
                labels = buckets.map { it.label },
                seriesA = buckets.map { it.collected },
                seriesB = buckets.map { it.expense },
                colorA = colors.gold,
                colorB = colors.gradientStart,
                height = if (large) 300.dp else 220.dp,
            )
            ChartLegend(
                entries = listOf("Collected" to colors.gold, "Expense" to colors.gradientStart),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Tablet stat tile: stretches to its row's height, with a larger value. */
@Composable
private fun TallStatCard(data: StatCardData, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    MaskanCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(MaskanDimens.statBadgeSize)
                        .background(data.tint.copy(alpha = 0.14f), RoundedCornerShape(MaskanDimens.statBadgeCornerRadius)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = data.icon, contentDescription = null, tint = data.tint, modifier = Modifier.size(16.dp))
                }
                Text(text = data.value, style = MaskanType.statValue.copy(fontSize = 34.sp, lineHeight = 40.sp), color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = data.label, style = MaskanType.secondary.copy(fontSize = 14.sp), color = colors.textSecondary)
        }
    }
}

/** Tablet ([large]): taller, with an icon and a larger label. */
@Composable
private fun QuickActionButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    large: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = modifier
            .height(if (large) 64.dp else 40.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (large && icon != null) {
            Icon(icon, contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = if (large) MaskanType.bodyMedium.copy(fontSize = 16.sp) else MaskanType.caption,
            color = colors.textPrimary,
        )
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
    fallbackCurrencyCode: String,
    onBillClick: (String) -> Unit,
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
                            DashboardBillRow(bill, property, today, property?.currency ?: fallbackCurrencyCode, onClick = { onBillClick(bill.id) })
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
private fun DashboardBillRow(bill: Bill, property: Property?, today: LocalDate, currencyCode: String, onClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val statusLabel = dueStatusLabel(bill, today)
    val statusColor = if (statusLabel == "Overdue") colors.danger else colors.warning
    val propertyName = property?.let { propertyDisplayName(it.buildingName, it.name, it.unit) } ?: ""

    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Column(modifier = Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
            Text(text = AmountFormatter.format(bill.amount, currencyCode), style = MaskanType.bodyMedium, color = colors.textPrimary)
            Text(text = statusLabel, style = MaskanType.caption, color = statusColor)
        }
    }
}

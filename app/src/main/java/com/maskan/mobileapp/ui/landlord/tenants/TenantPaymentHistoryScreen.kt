package com.maskan.mobileapp.ui.landlord.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.util.toLocalDate
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Full payment history for one tenant's property — reached via "View All" on Tenant Detail. */
@Composable
fun TenantPaymentHistoryScreen(viewModel: LandlordViewModel, tenantId: String, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    val tenant = tenants.find { it.id == tenantId }
    val property = tenant?.let { t -> properties.find { it.id == t.resolvedPropertyId } }
    val currencyCode = property?.currency ?: "USD"
    val billsById = remember(bills) { bills.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    val propertyPayments = remember(payments, property) {
        property?.let { p -> payments.filter { it.propertyId == p.id }.sortedByDescending { it.paidDate } }.orEmpty()
    }
    val groups = propertyPayments.groupBy { payment -> payment.paidDate?.toLocalDate()?.let { YearMonth.from(it) } }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
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
            Text(text = "Payment History", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.Center))
        }

        if (propertyPayments.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Receipt,
                title = "No payments yet",
                message = "This tenant's recorded payments will appear here.",
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                groups.entries.sortedByDescending { it.key }.forEach { (month, monthPayments) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = (month?.let { "${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.year}" } ?: "").uppercase(),
                            style = MaskanType.overline,
                            color = colors.textSecondary,
                        )
                        MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            monthPayments.forEachIndexed { index, payment ->
                                PaymentHistoryRow(payment, billsById[payment.billId]?.type, currencyCode, dateFormat)
                                if (index != monthPayments.lastIndex) {
                                    HorizontalDivider(color = colors.border)
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(bottom = 24.dp)) {}
            }
        }
    }
}

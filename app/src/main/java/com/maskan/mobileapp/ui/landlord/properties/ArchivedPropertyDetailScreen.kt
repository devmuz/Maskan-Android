package com.maskan.mobileapp.ui.landlord.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.ui.components.BillExpenseRow
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.billTypeColor
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Read-only detail for a soft-deleted property (ANDROID_PROPERTIES_FEATURE_SPEC.md
 * §5 / screenshots/deleted-property.png). No edit/delete actions — reached
 * from the Old tab on PropertiesScreen. Shows the last tenant on this
 * property (most recent by resolved property id, active or not, only when
 * one exists) and its full bill history.
 */
@Composable
fun ArchivedPropertyDetailScreen(
    viewModel: LandlordViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onDocumentsClick: (String) -> Unit,
    onBillClick: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    val archivedProperties by viewModel.archivedProperties.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val property = archivedProperties.find { it.id == propertyId }
    val currencyCode = property?.currency ?: viewModel.landlord.value?.currencyCode ?: "USD"
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    if (property == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Property not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val lastTenant = remember(tenants, propertyId) {
        tenants.filter { it.resolvedPropertyId == propertyId }
            .maxByOrNull { it.moveOutDate?.time ?: it.moveInDate?.time ?: 0L }
    }
    val propertyBills = remember(bills, propertyId) {
        bills.filter { it.propertyId == propertyId }.sortedByDescending { it.dueDate?.time ?: 0L }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            Text(text = property.displayBuildingName, style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(start = 4.dp))
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            if (!property.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = property.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(MaskanDimens.heroCornerRadius)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = property.displayBuildingName, style = MaskanType.sectionTitle, color = colors.textPrimary)
                        if (property.unit.isNotBlank()) {
                            Text(text = property.unit, style = MaskanType.body, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    StatusBadge(text = "Deleted", color = colors.danger)
                }
                property.propertyType?.let {
                    Text(text = it.label, style = MaskanType.bodyMedium, color = colors.gradientStart, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryGradient, RoundedCornerShape(MaskanDimens.cornerRadius))
                    .padding(MaskanDimens.cardPadding),
            ) {
                Text(text = "PROPERTY ID", style = MaskanType.overline, color = Color.White.copy(alpha = 0.8f))
                Text(text = property.propertyIdCode, style = MaskanType.sectionTitle, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }

            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Address", property.address)
                DetailRow("Monthly Rent", "${AmountFormatter.format(property.monthlyRent, currencyCode)} / month")
                property.electricityAccountNumber?.takeIf { it.isNotBlank() }?.let { DetailRow("Electricity account", it) }
                property.houseTaxNumber?.takeIf { it.isNotBlank() }?.let { DetailRow("House tax number", it) }
                property.waterTaxNumber?.takeIf { it.isNotBlank() }?.let { DetailRow("Water tax number", it) }
            }

            MaskanCard(
                modifier = Modifier.fillMaxWidth().clickable { onDocumentsClick(property.id) },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = colors.gradientStart)
                        Text(text = "Documents", style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(start = 10.dp))
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                }
            }

            if (lastTenant != null) {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "LAST TENANT", style = MaskanType.overline, color = colors.textTertiary)
                    DetailRow("Name", lastTenant.name)
                    lastTenant.moveOutDate?.let { DetailRow("Moved out", dateFormat.format(it)) }
                    StatusBadge(
                        text = if (lastTenant.isDeleted) "Removed" else "Moved Out",
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (propertyBills.isNotEmpty()) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(text = "BILLS & EXPENSES", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        propertyBills.forEachIndexed { index, bill ->
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
                            if (index != propertyBills.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = colors.border)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaskanType.body, color = colors.textSecondary)
        Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary)
    }
}

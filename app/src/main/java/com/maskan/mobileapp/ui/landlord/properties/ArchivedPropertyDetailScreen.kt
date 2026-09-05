package com.maskan.mobileapp.ui.landlord.properties

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Read-only detail for a soft-deleted property (feature-properties.md's
 * `ArchivedPropertyDetailView`). No edit/delete actions — reached from the
 * Old tab on PropertiesScreen. Shows the last tenant on this property (most
 * recent by resolved property id, active or not) and its bill history.
 */
@Composable
fun ArchivedPropertyDetailScreen(viewModel: LandlordViewModel, propertyId: String, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val archivedProperties by viewModel.archivedProperties.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val property = archivedProperties.find { it.id == propertyId }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
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
    val propertyBills = remember(bills, propertyId) { bills.filter { it.propertyId == propertyId } }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
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
                Text(text = property.displayBuildingName, style = MaskanType.sectionTitle, color = colors.textPrimary)
                if (property.unit.isNotBlank()) {
                    Text(text = property.unit, style = MaskanType.body, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                }
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    property.propertyType?.let { Text(text = it.label, style = MaskanType.bodyMedium, color = colors.gradientStart) }
                    StatusBadge(text = "Deleted", color = colors.textTertiary)
                }
            }

            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Property ID", property.propertyIdCode)
                DetailRow("Address", property.address)
                DetailRow("Monthly rent", "${numberFormat.format(property.monthlyRent)} / month")
            }

            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "LAST TENANT", style = MaskanType.overline, color = colors.textTertiary)
                if (lastTenant == null) {
                    Text(text = "No tenant history for this property.", style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp))
                } else {
                    DetailRow("Name", lastTenant.name)
                    lastTenant.moveOutDate?.let { DetailRow("Moved out", dateFormat.format(it)) }
                }
            }

            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(text = "BILLS & EXPENSES", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                if (propertyBills.isEmpty()) {
                    Text(text = "No bill history for this property.", style = MaskanType.secondary, color = colors.textSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        propertyBills.sortedByDescending { it.dueDate }.forEach { bill ->
                            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = bill.type.label, style = MaskanType.bodyMedium, color = colors.textPrimary)
                                    Text(text = numberFormat.format(bill.amount), style = MaskanType.bodyMedium, color = colors.textPrimary)
                                }
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

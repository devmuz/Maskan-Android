package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.data.model.Bill
import com.maskan.mobileapp.data.model.BillStatus
import com.maskan.mobileapp.data.model.ServiceRequest
import com.maskan.mobileapp.data.model.ServiceRequestCategory
import com.maskan.mobileapp.data.model.ServiceRequestStatus
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.data.util.startOfToday
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.billTypeIcon
import com.maskan.mobileapp.ui.theme.MaskanColors
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Locale

/** Paid/Verifying/Overdue/Pending — "Overdue" computed client-side, not read off `status` (09-tenant-app.md). */
@Composable
fun tenantBillStatusLabelAndColor(bill: Bill): Pair<String, Color> {
    val colors = MaskanTheme.colors
    return when {
        bill.status == BillStatus.PAID -> "Paid" to colors.success
        bill.status == BillStatus.VERIFYING -> "Verifying" to colors.warning
        bill.isActuallyOverdue(startOfToday()) -> "Overdue" to colors.danger
        else -> "Pending" to colors.warning
    }
}

fun serviceRequestStatusColor(status: ServiceRequestStatus, colors: MaskanColors): Color = when (status) {
    ServiceRequestStatus.PENDING -> colors.warning
    ServiceRequestStatus.IN_PROGRESS -> colors.gradientStart
    ServiceRequestStatus.RESOLVED -> colors.success
}

private val dateFormat by lazy { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

@Composable
fun BillRow(bill: Bill, currencyCode: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val (statusLabel, statusColor) = tenantBillStatusLabelAndColor(bill)
    val dateLabel = if (bill.status == BillStatus.PAID) {
        bill.paidAt?.let { "Paid ${dateFormat.format(it)}" } ?: "Paid"
    } else {
        bill.dueDate?.let { "Due ${dateFormat.format(it)}" } ?: ""
    }

    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(colors.gradientStart.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(billTypeIcon(bill.type), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = bill.type.label, style = MaskanType.bodyMedium, color = colors.textPrimary)
                Text(text = dateLabel, style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = AmountFormatter.format(bill.amount, currencyCode), style = MaskanType.bodyMedium, color = colors.textPrimary)
            Text(text = statusLabel, style = MaskanType.caption, color = statusColor)
        }
    }
}

fun requestCategoryIcon(category: ServiceRequestCategory): ImageVector = when (category) {
    ServiceRequestCategory.PLUMBING -> Icons.Filled.Plumbing
    ServiceRequestCategory.ELECTRICAL -> Icons.Filled.Bolt
    ServiceRequestCategory.APPLIANCE -> Icons.Filled.Kitchen
    ServiceRequestCategory.GENERAL -> Icons.Filled.Build
    ServiceRequestCategory.OTHER -> Icons.Filled.MoreHoriz
}

@Composable
fun RequestRow(request: ServiceRequest, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val statusColor = serviceRequestStatusColor(request.status, colors)
    val dateLabel = request.createdAt?.let { dateFormat.format(it) } ?: ""

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(colors.gradientStart.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(requestCategoryIcon(request.category), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = request.title, style = MaskanType.bodyMedium, color = colors.textPrimary)
                Text(text = "${request.category.label} · $dateLabel", style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
        StatusBadge(text = request.status.label, color = statusColor)
    }
}

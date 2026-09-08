package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared bill/expense row — icon square, title + date, amount + status pill.
 * Used by Building Detail's "Property Expenses" and Archived Property
 * Detail's "Bills & Expenses" (both read the same shape from a screenshot
 * reference: deleted-property.png).
 */
@Composable
fun BillExpenseRow(
    title: String,
    date: Date?,
    amount: String,
    statusLabel: String,
    statusColor: Color,
    iconTint: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    dueDate: Date? = null,
) {
    val colors = MaskanTheme.colors
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, style = MaskanType.bodyMedium, color = colors.textPrimary)
                val dateText = buildString {
                    append(date?.let { dateFormat.format(it) } ?: "—")
                    dueDate?.let { append(" · Due ${dateFormat.format(it)}") }
                }
                Text(text = dateText, style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = amount, style = MaskanType.bodyMedium, color = colors.textPrimary)
            StatusBadge(text = statusLabel, color = statusColor, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

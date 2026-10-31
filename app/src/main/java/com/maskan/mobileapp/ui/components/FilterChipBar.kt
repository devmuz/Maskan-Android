package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Horizontally scrolling capsule filter chips (selected = primary gradient,
 * others = field background) — the iOS filter style used for Properties /
 * Tenants (Active/Old), Bills and Requests status filters, and Add Bill's
 * frequency. Settings → Theme keeps [SegmentedControl], as on iOS.
 */
@Composable
fun <T> FilterChipBar(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colors.primaryGradient else SolidColor(colors.fieldBackground))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = label(option),
                    style = MaskanType.bodyMedium,
                    color = if (isSelected) Color.White else colors.textPrimary,
                )
            }
        }
    }
}

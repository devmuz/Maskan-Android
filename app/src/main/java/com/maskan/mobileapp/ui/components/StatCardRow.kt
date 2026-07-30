package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

data class StatCardData(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val tint: Color,
)

/**
 * Dashboard stat grid rule (04-landlord-dashboard.md): chunk into groups of 3,
 * each row's cards get equal weight — so a trailing row of 1-2 cards still
 * fills the row width instead of leaving a gap. A fixed-column grid would not
 * reproduce this; hence explicit per-chunk equal-weight Rows.
 */
@Composable
fun StatCardGrid(cards: List<StatCardData>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing)) {
        cards.chunked(3).forEach { chunk ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing),
                modifier = Modifier.fillMaxWidth(),
            ) {
                chunk.forEach { card ->
                    StatCard(card, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(data: StatCardData, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    MaskanCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(MaskanDimens.statBadgeSize)
                .background(data.tint.copy(alpha = 0.14f), RoundedCornerShape(MaskanDimens.statBadgeCornerRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = data.icon, contentDescription = null, tint = data.tint, modifier = Modifier.size(16.dp))
        }
        Text(text = data.value, style = MaskanType.statValue, color = colors.textPrimary, modifier = Modifier.padding(top = 14.dp))
        Text(text = data.label, style = MaskanType.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
    }
}

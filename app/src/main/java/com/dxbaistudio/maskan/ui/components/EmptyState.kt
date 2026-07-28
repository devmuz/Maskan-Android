package com.dxbaistudio.maskan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dxbaistudio.maskan.ui.theme.MaskanDimens
import com.dxbaistudio.maskan.ui.theme.MaskanTheme
import com.dxbaistudio.maskan.ui.theme.MaskanType

/** Centered empty-state pattern shared across list screens (01-design-system.md). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaskanTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = MaskanDimens.screenHPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(MaskanDimens.emptyStateBadgeSize)
                .background(colors.gradientStart.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.gradientStart,
                modifier = Modifier.size(MaskanDimens.emptyStateIconSize),
            )
        }
        Text(text = title, style = MaskanType.sectionTitle.copy(fontSize = 20.sp), color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(text = message, style = MaskanType.body.copy(fontSize = 15.sp), color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

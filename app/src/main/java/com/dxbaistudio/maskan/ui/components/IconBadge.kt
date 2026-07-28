package com.dxbaistudio.maskan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dxbaistudio.maskan.ui.theme.MaskanTheme

/** Rounded-square icon badge used in screen headers (Auth screens, Property Detail, etc). */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 18.dp,
    iconSize: Dp = 26.dp,
    brush: Brush? = null,
    iconTint: Color = Color.White,
) {
    val colors = MaskanTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 12.dp, shape = shape, ambientColor = colors.indigoDeep.copy(alpha = 0.4f), spotColor = colors.indigoDeep.copy(alpha = 0.4f))
            .background(brush ?: colors.primaryGradient, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}

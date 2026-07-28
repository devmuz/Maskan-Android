package com.dxbaistudio.maskan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dxbaistudio.maskan.ui.theme.MaskanDimens
import com.dxbaistudio.maskan.ui.theme.MaskanTheme
import com.dxbaistudio.maskan.ui.theme.MaskanType

/**
 * "GradientButton" primary CTA. Defaults to the gold `accentGradient`, per the
 * 60/30/10 rule reserved for the one primary action on a screen. Pass
 * [brush] = MaskanTheme.colors.primaryGradient for the two top-level identity
 * buttons on Role Selection, which are an intentional exception.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null,
    brush: Brush? = null,
    contentColor: Color? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = MaskanTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = enabled && !isLoading

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "buttonScale",
    )
    val contentAlpha = when {
        !isActive -> 0.6f
        isPressed -> 0.9f
        else -> 1f
    }

    val resolvedBrush = brush ?: colors.accentGradient
    val resolvedContentColor = contentColor ?: colors.onAccent
    val shadowTint = if (brush == null) colors.accentGradientShadowTint.copy(alpha = 0.35f) else colors.shadowColor
    val shape = RoundedCornerShape(MaskanDimens.cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MaskanDimens.buttonHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = 16.dp, shape = shape, ambientColor = shadowTint, spotColor = shadowTint)
            .clip(shape)
            .background(resolvedBrush)
            .border(BorderStroke(0.75.dp, Color.White.copy(alpha = 0.35f)), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isActive,
                onClick = onClick,
            )
            .alpha(contentAlpha),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = MaskanDimens.cardPadding),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        color = resolvedContentColor,
                        strokeWidth = 2.dp,
                    )
                    Text(text = loadingText ?: text, style = MaskanType.buttonLabel, color = resolvedContentColor)
                } else {
                    leadingIcon?.invoke()
                    Text(text = text, style = MaskanType.buttonLabel, color = resolvedContentColor)
                }
            }
        }
    }
}

package com.maskan.mobileapp.ui.coachmark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.DarkMaskanColors
import com.maskan.mobileapp.ui.theme.LightMaskanColors
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlin.math.roundToInt

/**
 * Popup card with a small arrow pointing at [targetRect], placed above or below it depending
 * on available space. No dimming/highlight ring — matches the final iOS design. The card's
 * own real height is measured via [onGloballyPositioned] rather than guessed, so short
 * descriptions don't leave a gap between the card and the target (a real bug hit on iOS).
 */
@Composable
fun CoachMarkOverlay(
    step: CoachMarkStep,
    stepNumber: Int,
    totalSteps: Int,
    targetRect: Rect?,
    canGoPrevious: Boolean,
    isLastStep: Boolean,
    showsNext: Boolean = true,
    showsStepCounter: Boolean = true,
    showsSkip: Boolean = true,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (targetRect == null) return

    val colors = MaskanTheme.colors
    val density = LocalDensity.current
    val cardBackground = if (colors.isDark) LightMaskanColors.surface else DarkMaskanColors.surface
    val cardTextPrimary = if (colors.isDark) LightMaskanColors.textPrimary else DarkMaskanColors.textPrimary
    val cardTextSecondary = if (colors.isDark) LightMaskanColors.textSecondary else DarkMaskanColors.textSecondary

    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                containerOrigin = it.positionInRoot()
                containerSize = it.size
            },
    ) {
        if (containerSize == IntSize.Zero) return@Box

        val localTarget = targetRect.translate(-containerOrigin.x, -containerOrigin.y)

        val spacingPx = with(density) { 10.dp.toPx() }
        val marginPx = with(density) { 20.dp.toPx() }
        val cardWidthPx = minOf(with(density) { 330.dp.toPx() }, containerSize.width - marginPx * 2)
        // Falls back to a guess only until the card is first measured — the real
        // measured height (below) immediately replaces it on the next frame.
        val cardHeightPx = if (cardSize.height > 0) cardSize.height.toFloat() else with(density) { 150.dp.toPx() }

        val placeBelow = localTarget.bottom + spacingPx + cardHeightPx <= containerSize.height
        val cardTop = if (placeBelow) localTarget.bottom + spacingPx else localTarget.top - spacingPx - cardHeightPx

        val halfCardWidth = cardWidthPx / 2f
        val cardLeft = if (containerSize.width - marginPx * 2 <= cardWidthPx) {
            marginPx
        } else {
            (localTarget.center.x - halfCardWidth).coerceIn(marginPx, containerSize.width - marginPx - cardWidthPx)
        }

        val arrowWidthPx = with(density) { 16.dp.toPx() }
        val arrowHeightPx = with(density) { 8.dp.toPx() }
        val arrowCenterX = (localTarget.center.x - cardLeft).coerceIn(arrowWidthPx, cardWidthPx - arrowWidthPx)
        val arrowLeft = cardLeft + arrowCenterX - arrowWidthPx / 2f
        val arrowTop = if (placeBelow) cardTop - arrowHeightPx else cardTop + cardHeightPx

        Canvas(
            modifier = Modifier
                .offset { IntOffset(arrowLeft.roundToInt(), arrowTop.roundToInt()) }
                .size(width = with(density) { arrowWidthPx.toDp() }, height = with(density) { arrowHeightPx.toDp() }),
        ) {
            val path = Path().apply {
                if (placeBelow) {
                    moveTo(size.width / 2f, 0f)
                    lineTo(0f, size.height)
                    lineTo(size.width, size.height)
                } else {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                }
                close()
            }
            drawPath(path, cardBackground)
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(cardLeft.roundToInt(), cardTop.roundToInt()) }
                .width(with(density) { cardWidthPx.toDp() })
                .onGloballyPositioned { cardSize = it.size }
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground)
                .padding(16.dp),
        ) {
            if (showsStepCounter) {
                androidx.compose.material3.Text(
                    text = "Step $stepNumber of $totalSteps",
                    style = MaskanType.overline,
                    color = cardTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            androidx.compose.material3.Text(
                text = step.description,
                style = MaskanType.body,
                color = cardTextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (canGoPrevious) {
                    androidx.compose.material3.Text(
                        text = "Previous",
                        style = MaskanType.bodyMedium,
                        color = cardTextSecondary,
                        modifier = Modifier.clickable(onClick = onPrevious),
                    )
                } else {
                    Box(modifier = Modifier)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (showsSkip) {
                        androidx.compose.material3.Text(
                            text = "Skip",
                            style = MaskanType.bodyMedium,
                            color = cardTextSecondary,
                            modifier = Modifier.clickable(onClick = onSkip),
                        )
                    }
                    if (showsNext) {
                        androidx.compose.material3.Text(
                            text = if (isLastStep) "Done" else "Next",
                            style = MaskanType.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.gradientStart,
                            modifier = Modifier.clickable(onClick = onNext),
                        )
                    }
                }
            }
        }
    }
}

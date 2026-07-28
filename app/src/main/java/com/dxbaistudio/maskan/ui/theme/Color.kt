package com.dxbaistudio.maskan.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Full adaptive palette, ported 1:1 from the iOS Theme.swift hex values
 * (see android-port/01-design-system.md). Every token below has a light
 * and dark variant except the two "fixed" colors which are intentionally
 * non-adaptive.
 */
@Immutable
data class MaskanColors(
    val background: Color,
    val surface: Color,
    val fieldBackground: Color,
    val titleBackground: Color,
    val border: Color,
    val shadowColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val indigoDeep: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val gold: Color,
    val onAccent: Color,
    val isDark: Boolean,
) {
    /** Hero surfaces: feature cards, logo mark, large stat panels. Same in both themes. */
    val primaryGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(Color(0xFF33297A), Color(0xFF753085)),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    /** Primary call-to-action buttons only (~10% accent). Same in both themes. */
    val accentGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(Color(0xFFF2C24F), Color(0xFFD98F2B)),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    /** Darker stop of [accentGradient], used for the gold button's tinted shadow. */
    val accentGradientShadowTint: Color
        get() = Color(0xFFD98F2B)
}

val LightMaskanColors = MaskanColors(
    background = Color(0xFFF6F6F9),
    surface = Color(0xFFFFFFFF),
    fieldBackground = Color(0xFFF3F3F7),
    titleBackground = Color(0xFFEDF0FA),
    border = Color.Black.copy(alpha = 0.06f),
    shadowColor = Color(0xFF332E59).copy(alpha = 0.14f),
    textPrimary = Color(0xFF171A29),
    textSecondary = Color(0xFF707387),
    textTertiary = Color(0xFFA1A3B2),
    gradientStart = Color(0xFF4A389E),
    gradientEnd = Color(0xFF853394),
    indigoDeep = Color(0xFF33297A),
    success = Color(0xFF1C944F),
    warning = Color(0xFFDE850D),
    danger = Color(0xFFD1333D),
    gold = Color(0xFFB87A0F),
    onAccent = Color(0xFF291C0A),
    isDark = false,
)

val DarkMaskanColors = MaskanColors(
    background = Color(0xFF0B0C14),
    surface = Color(0xFF1B1D2A),
    fieldBackground = Color(0xFF232534),
    titleBackground = Color(0xFF161825),
    border = Color.White.copy(alpha = 0.09f),
    shadowColor = Color.Black.copy(alpha = 0.55f),
    textPrimary = Color(0xFFF7F7F7),
    textSecondary = Color(0xFF9EA1B8),
    textTertiary = Color(0xFF6B6E85),
    gradientStart = Color(0xFFAD99FF),
    gradientEnd = Color(0xFFDB85DB),
    indigoDeep = Color(0xFF33297A),
    success = Color(0xFF4CD182),
    warning = Color(0xFFFFAD4C),
    danger = Color(0xFFFF6B6B),
    gold = Color(0xFFF2C24F),
    onAccent = Color(0xFF291C0A),
    isDark = true,
)

val LocalMaskanColors = staticCompositionLocalOf { LightMaskanColors }

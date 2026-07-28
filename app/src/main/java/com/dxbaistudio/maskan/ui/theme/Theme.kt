package com.dxbaistudio.maskan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = DarkMaskanColors.gradientStart,
    onPrimary = DarkMaskanColors.textPrimary,
    secondary = DarkMaskanColors.gradientEnd,
    background = DarkMaskanColors.background,
    onBackground = DarkMaskanColors.textPrimary,
    surface = DarkMaskanColors.surface,
    onSurface = DarkMaskanColors.textPrimary,
    error = DarkMaskanColors.danger,
)

private val LightColorScheme = lightColorScheme(
    primary = LightMaskanColors.gradientStart,
    onPrimary = LightMaskanColors.surface,
    secondary = LightMaskanColors.gradientEnd,
    background = LightMaskanColors.background,
    onBackground = LightMaskanColors.textPrimary,
    surface = LightMaskanColors.surface,
    onSurface = LightMaskanColors.textPrimary,
    error = LightMaskanColors.danger,
)

/** Global accessor mirroring `MaterialTheme` (e.g. `MaskanTheme.colors.textPrimary`). */
object MaskanTheme {
    val colors: MaskanColors
        @Composable
        get() = LocalMaskanColors.current
}

@Composable
fun MaskanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val maskanColors = if (darkTheme) DarkMaskanColors else LightMaskanColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalMaskanColors provides maskanColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

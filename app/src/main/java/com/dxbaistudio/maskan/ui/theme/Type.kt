package com.dxbaistudio.maskan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Custom type roles from android-port/01-design-system.md. These don't map
 * cleanly onto Material3's default type scale (e.g. "Stat value", "Overline"),
 * so the app reads from this object directly rather than MaterialTheme.typography
 * for most screen-specific text.
 */
@Immutable
object MaskanType {
    val screenTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp)
    val sectionTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
    val cardTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val secondary = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val overline = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    )
    val statValue = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)
    val fieldLabel = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
    val buttonLabel = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val badge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
}

// Material3 Typography kept in sync with the roles above, for components (e.g. TextField)
// that read MaterialTheme.typography internally.
val Typography = Typography(
    headlineLarge = MaskanType.screenTitle,
    titleLarge = MaskanType.sectionTitle,
    titleMedium = MaskanType.cardTitle,
    bodyLarge = MaskanType.body,
    bodyMedium = MaskanType.bodyMedium,
    bodySmall = MaskanType.secondary,
    labelSmall = MaskanType.caption,
    labelMedium = MaskanType.badge,
)

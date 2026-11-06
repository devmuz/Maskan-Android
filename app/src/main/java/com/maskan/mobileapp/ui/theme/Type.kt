package com.maskan.mobileapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maskan.mobileapp.R

/**
 * App typeface: Plus Jakarta Sans (SIL Open Font License — see
 * assets/licenses/PlusJakartaSans-OFL.txt), bundled as static weights so it
 * renders the same on every device, offline, without Google Play services.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

/**
 * Custom type roles from docs/01-design-system.md. These don't map
 * cleanly onto Material3's default type scale (e.g. "Stat value", "Overline"),
 * so the app reads from this object directly rather than MaterialTheme.typography
 * for most screen-specific text. Sizes track the iOS app's point sizes
 * (secondary 13, body 15, message 14, overline 13) so both look the same.
 */
@Immutable
object MaskanType {
    val screenTitle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp)
    val sectionTitle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
    val cardTitle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    /** Notices, empty-state and helper messages — iOS uses 14pt for these. */
    val message = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val secondary = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val overline = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.5.sp,
    )
    val statValue = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)
    val fieldLabel = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)
    val buttonLabel = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val badge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp)
}

// Material3 Typography kept in sync with the roles above, for components (e.g. TextField)
// that read MaterialTheme.typography internally. Every other Material role (dialog
// titles, text buttons, menus…) also gets the app font.
private val baseTypography = Typography()

val Typography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = PlusJakartaSans),
    headlineLarge = MaskanType.screenTitle,
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = PlusJakartaSans),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = PlusJakartaSans),
    titleLarge = MaskanType.sectionTitle,
    titleMedium = MaskanType.cardTitle,
    titleSmall = baseTypography.titleSmall.copy(fontFamily = PlusJakartaSans),
    bodyLarge = MaskanType.body,
    bodyMedium = MaskanType.bodyMedium,
    bodySmall = MaskanType.secondary,
    labelLarge = baseTypography.labelLarge.copy(fontFamily = PlusJakartaSans),
    labelMedium = MaskanType.badge,
    labelSmall = MaskanType.caption,
)

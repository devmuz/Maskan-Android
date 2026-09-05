package com.maskan.mobileapp.ui.theme

import androidx.compose.ui.unit.dp

/** 8-point grid spacing & metrics, from android-port/01-design-system.md. */
object MaskanDimens {
    val cornerRadius = 18.dp
    val heroCornerRadius = 28.dp
    val buttonHeight = 56.dp
    val fieldHeight = 52.dp
    val cardPadding = 18.dp
    val sectionSpacing = 20.dp
    val itemSpacing = 12.dp
    val screenHPadding = 20.dp

    val cardShadowRadius = 20.dp
    val cardShadowOffsetY = 8.dp
    val rowShadowRadius = 8.dp
    val rowShadowOffsetY = 3.dp

    val statBadgeSize = 32.dp
    val statBadgeCornerRadius = 10.dp
    val emptyStateBadgeSize = 88.dp
    val emptyStateIconSize = 34.dp

    // Floating tab bar (landlord shell): height of the pill itself, its margin
    // from the screen edges/system nav bar, and how much bottom clearance scrollable
    // tab-root content needs to reserve so its last item can clear the floating pill.
    val glassBarHeight = 68.dp
    val glassBarHMargin = 12.dp
    val glassBarBottomMargin = 12.dp
    val glassBarContentClearance = glassBarHeight + glassBarBottomMargin + 16.dp
}

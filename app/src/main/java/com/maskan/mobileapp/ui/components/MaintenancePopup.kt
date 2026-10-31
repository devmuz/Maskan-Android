package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Blocking (or dismissible, depending on [isAppAccessible]) popup shown over
 * the splash/app content when config/app.scheduleMaintenance is true. Mirrors
 * Maskan-iOS's MaintenancePopupView.
 */
@Composable
fun MaintenancePopup(message: String, isAppAccessible: Boolean, onContinue: () -> Unit) {
    val colors = MaskanTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            // A plain Box with just a background doesn't consume touches in
            // Compose — without this, taps pass straight through to whatever
            // is rendered behind it (confirmed: tapping "through" the scrim
            // navigated the screen underneath). This is the standard modal-
            // scrim pattern: an empty onClick that only exists to swallow
            // the gesture, no ripple since this isn't a real button.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        MaskanCard(
            modifier = Modifier.widthIn(max = 420.dp).padding(horizontal = 20.dp),
            cornerRadius = MaskanDimens.heroCornerRadius,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(colors.primaryGradient, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Text(
                    text = "App Maintenance",
                    style = MaskanType.sectionTitle,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = message,
                    style = MaskanType.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )

                if (isAppAccessible) {
                    GradientButton(
                        text = "Continue",
                        onClick = onContinue,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        }
    }
}

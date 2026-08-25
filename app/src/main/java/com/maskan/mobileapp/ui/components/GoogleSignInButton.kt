package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/** "or" divider between the primary form action and Google sign-in (03-auth.md). */
@Composable
fun OrDivider() {
    val colors = MaskanTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
        Text(text = "or", style = MaskanType.secondary, color = colors.textSecondary)
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
    }
}

/** Google brand red for the "G" glyph, per 03-auth.md (#FA4F44) — ported 1:1 from the iOS button, not a themed token. */
private val GoogleRed = Color(0xFFFA4F44)

/**
 * Outlined "Continue with Google" button (03-auth.md): system-background fill,
 * bordered stroke, bold red "G" + medium-weight label — not the multi-color
 * Google logo mark. Disabled while [isLoading], with no extra dimming beyond that.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val colors = MaskanTheme.colors
    val shape = RoundedCornerShape(MaskanDimens.cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MaskanDimens.buttonHeight)
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.5.dp, colors.border), shape)
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.textSecondary, strokeWidth = 2.dp)
            } else {
                Text(text = "G", style = MaskanType.buttonLabel.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = GoogleRed)
            }
            Text(
                text = "Continue with Google",
                style = MaskanType.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
            )
        }
    }
}

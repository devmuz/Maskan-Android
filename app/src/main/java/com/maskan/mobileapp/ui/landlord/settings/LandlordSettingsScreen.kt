package com.maskan.mobileapp.ui.landlord.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Intentionally minimal (08-landlord-settings.md): avatar + email + sign out.
 * No account editing / notification-preferences / app-info content today.
 */
@Composable
fun LandlordSettingsScreen(viewModel: LandlordViewModel, onSignedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val colors = MaskanTheme.colors
    val email = container.authRepository.currentUser?.email ?: "Landlord"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Settings", style = MaskanType.screenTitle, color = colors.textPrimary, modifier = Modifier.padding(bottom = 32.dp))

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(16.dp, CircleShape, ambientColor = colors.indigoDeep, spotColor = colors.indigoDeep)
                .background(colors.primaryGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Text(text = email, style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 16.dp))

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        GradientButton(
            text = "Log Out",
            onClick = {
                viewModel.signOut()
                onSignedOut()
            },
            brush = androidx.compose.ui.graphics.SolidColor(colors.fieldBackground),
            contentColor = colors.danger,
        )
    }
}

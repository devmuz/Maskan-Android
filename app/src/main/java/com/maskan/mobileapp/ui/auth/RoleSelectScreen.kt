package com.maskan.mobileapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import com.maskan.mobileapp.ui.nav.Routes

@Composable
fun RoleSelectScreen(navController: NavHostController) {
    val colors = MaskanTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // Soft blurred glow behind the logo mark.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .size(280.dp)
                .blur(90.dp)
                .background(colors.gradientStart.copy(alpha = 0.35f), CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(20.dp, RoundedCornerShape(28.dp), ambientColor = colors.indigoDeep, spotColor = colors.indigoDeep)
                    .background(colors.primaryGradient, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.House, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Text(
                text = "Maskan",
                style = MaskanType.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = "Property management, made simple",
                style = MaskanType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(
                text = "CONTINUE AS",
                style = MaskanType.overline,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 56.dp, bottom = 16.dp),
            )

            // These two identity buttons intentionally use primaryGradient, not the
            // gold accentGradient — they're navigation choices, not "the one CTA"
            // the way a form's submit button is (03-auth.md).
            GradientButton(
                text = "I am a Landlord",
                onClick = { navController.navigate(Routes.LANDLORD_LOGIN) },
                brush = colors.primaryGradient,
                contentColor = Color.White,
                leadingIcon = { Icon(Icons.Filled.Apartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
            )
            Box(modifier = Modifier.padding(top = 14.dp).fillMaxWidth()) {
                GradientButton(
                    text = "I am a Tenant",
                    onClick = { navController.navigate(Routes.TENANT_LOGIN) },
                    brush = colors.primaryGradient,
                    contentColor = Color.White,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                )
            }
        }
    }
}

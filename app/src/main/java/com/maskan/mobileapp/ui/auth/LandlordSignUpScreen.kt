package com.maskan.mobileapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maskan.mobileapp.data.prefs.UserRole
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.nav.Routes
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

@Composable
fun LandlordSignUpScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isValid = email.isNotBlank() && password.length >= 6 && password == confirmPassword

    AuthScaffold(icon = Icons.Filled.PersonAddAlt, title = "Create account", subtitle = "Start managing your properties with Maskan") {
        AppTextField(value = email, onValueChange = { email = it; error = null }, label = "Email", keyboardType = KeyboardType.Email)
        AppTextField(value = password, onValueChange = { password = it; error = null }, label = "Password (min 6 characters)", isPassword = true)
        AppTextField(value = confirmPassword, onValueChange = { confirmPassword = it; error = null }, label = "Confirm Password", isPassword = true)

        error?.let {
            Text(text = it, style = MaskanType.secondary.copy(fontWeight = FontWeight.Medium), color = colors.danger)
        }

        GradientButton(
            text = "Sign Up",
            isLoading = isLoading,
            loadingText = "Creating Account…",
            enabled = isValid,
            onClick = {
                error = null
                isLoading = true
                scope.launch {
                    try {
                        container.authRepository.signUpLandlord(email, password)
                        container.rolePreferences.setRole(UserRole.LANDLORD)
                        navController.navigate(Routes.LANDLORD_SHELL) { popUpTo(0) { inclusive = true } }
                    } catch (t: Throwable) {
                        error = t.message ?: "Something went wrong. Please try again."
                    } finally {
                        isLoading = false
                    }
                }
            },
        )

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
            Text(text = "Already have an account? ", style = MaskanType.secondary, color = colors.textSecondary)
            Text(
                text = "Log in",
                style = MaskanType.secondary,
                color = colors.gradientStart,
                modifier = Modifier.clickable { navController.popBackStack() },
            )
        }
    }
}

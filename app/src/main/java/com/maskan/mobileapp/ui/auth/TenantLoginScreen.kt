package com.maskan.mobileapp.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun TenantLoginScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()

    var propertyId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isValid = propertyId.isNotBlank() && password.isNotBlank()

    AuthScaffold(icon = Icons.Filled.Person, title = "Tenant login", subtitle = "Use the Property ID and password provided by your landlord") {
        AppTextField(
            value = propertyId,
            onValueChange = { propertyId = it.uppercase(); error = null },
            label = "Property ID",
            placeholder = "ABCDE123",
        )
        AppTextField(value = password, onValueChange = { password = it; error = null }, label = "Password", isPassword = true)

        error?.let {
            Text(text = it, style = MaskanType.secondary.copy(fontWeight = FontWeight.Medium), color = colors.danger)
        }

        GradientButton(
            text = "Log In",
            isLoading = isLoading,
            loadingText = "Logging In…",
            enabled = isValid,
            onClick = {
                error = null
                isLoading = true
                scope.launch {
                    try {
                        val code = container.authRepository.signInTenant(propertyId, password)
                        container.rolePreferences.setRole(UserRole.TENANT)
                        container.rolePreferences.setTenantPropertyIdCode(code)
                        navController.navigate(Routes.TENANT_SHELL) { popUpTo(0) { inclusive = true } }
                    } catch (t: Throwable) {
                        error = t.message ?: "Something went wrong. Please try again."
                    } finally {
                        isLoading = false
                    }
                }
            },
        )
    }
}

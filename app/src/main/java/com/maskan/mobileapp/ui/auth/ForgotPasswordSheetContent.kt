package com.maskan.mobileapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

/**
 * Firebase's sendPasswordResetEmail succeeds silently even if no account
 * exists (anti-enumeration) — there is no "no account found" error state to
 * build (03-auth.md).
 */
@Composable
fun ForgotPasswordSheetContent(prefilledEmail: String, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf(prefilledEmail) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var succeeded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (succeeded) {
            Box(
                modifier = Modifier.size(64.dp).background(colors.success.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(30.dp))
            }
            Text(text = "Check your email", style = MaskanType.sectionTitle, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(
                text = "If an account exists for $email, a reset link is on its way.",
                style = MaskanType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            GradientButton(text = "Done", onClick = onDone, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        } else {
            Box(
                modifier = Modifier.size(56.dp).background(colors.primaryGradient, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Text(text = "Forgot your password?", style = MaskanType.sectionTitle, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(
                text = "Enter your account email and we'll send you a link to reset it.",
                style = MaskanType.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            AppTextField(value = email, onValueChange = { email = it; error = null }, label = "Email", keyboardType = KeyboardType.Email)
            error?.let { Text(text = it, style = MaskanType.secondary, color = colors.danger) }
            GradientButton(
                text = "Send Reset Link",
                isLoading = isLoading,
                loadingText = "Sending…",
                enabled = email.isNotBlank(),
                onClick = {
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            container.authRepository.sendPasswordReset(email)
                            succeeded = true
                        } catch (t: Throwable) {
                            error = t.message ?: "Something went wrong. Please try again."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

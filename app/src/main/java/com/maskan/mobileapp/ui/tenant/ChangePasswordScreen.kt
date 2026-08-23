package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.components.AppTextField
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

/**
 * Verified and rewritten entirely client-side against the stored salted
 * hash (09-tenant-app.md / 02-data-models.md). No landlord-side equivalent
 * — landlords use Firebase Auth's forgot-password email flow instead.
 */
@Composable
fun ChangePasswordScreen(viewModel: TenantSessionViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isValid = newPassword.length >= 6 && newPassword == confirmPassword && newPassword != currentPassword && currentPassword.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(horizontal = MaskanDimens.screenHPadding)) {
        Text(
            text = "Change Password",
            style = MaskanType.screenTitle,
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = 24.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing)) {
            AppTextField(value = currentPassword, onValueChange = { currentPassword = it; error = null }, label = "Current Password", isPassword = true)
            AppTextField(value = newPassword, onValueChange = { newPassword = it; error = null }, label = "New Password", isPassword = true)
            AppTextField(value = confirmPassword, onValueChange = { confirmPassword = it; error = null }, label = "Confirm New Password", isPassword = true)

            error?.let { Text(text = it, style = MaskanType.secondary.copy(fontWeight = FontWeight.Medium), color = colors.danger) }

            GradientButton(
                text = "Save",
                isLoading = isSaving,
                loadingText = "Saving…",
                enabled = isValid,
                onClick = {
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            viewModel.changePassword(currentPassword, newPassword)
                            onDone()
                        } catch (t: Throwable) {
                            error = t.message ?: "Couldn't change your password."
                        } finally {
                            isSaving = false
                        }
                    }
                },
            )

            Text(
                text = "Cancel",
                style = MaskanType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onCancel),
            )
        }
    }
}

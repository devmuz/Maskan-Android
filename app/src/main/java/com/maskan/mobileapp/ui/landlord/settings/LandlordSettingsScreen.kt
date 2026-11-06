package com.maskan.mobileapp.ui.landlord.settings

import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.scale
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Brush
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.maskan.mobileapp.ui.components.isExpandedWidth
import com.maskan.mobileapp.ui.components.SettingsTabletLayout
import com.maskan.mobileapp.ui.components.SettingsSkyline
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.Currencies
import com.maskan.mobileapp.data.prefs.AppTheme
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.SegmentedControl
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.LocalLandlordContentBottomInset
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch

/** Full 6-section layout (08-landlord-settings.md) — no longer the near-empty placeholder. */
@Composable
fun LandlordSettingsScreen(
    viewModel: LandlordViewModel,
    onOpenCurrencyPicker: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val container = LocalAppContainer.current
    val colors = MaskanTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val email = container.authRepository.currentUser?.email ?: "Landlord"
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val theme by container.themePreferences.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val currentCurrency = remember(landlord?.preferredCurrency) { Currencies.byCode(landlord?.preferredCurrency) }

    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var showNotificationsBlockedDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountFinalDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        if (!granted) showNotificationsBlockedDialog = true
    }
    LaunchedEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val expanded = isExpandedWidth()
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

    val header: @Composable () -> Unit = {
        // 1. Header (avatar + email, as on iOS)
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(16.dp, CircleShape, ambientColor = colors.indigoDeep, spotColor = colors.indigoDeep)
                    .background(colors.primaryGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Text(text = email, style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 16.dp))
        }
    }
    val appearance: @Composable () -> Unit = {
        // 2. Appearance
        SettingsSection("APPEARANCE") {
            SettingsRow(icon = Icons.Filled.Brush, iconColor = SettingsColors.purple, title = "Theme")
            SegmentedControl(
                options = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK),
                selected = theme,
                onSelect = { scope.launch { container.themePreferences.setTheme(it) } },
                label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
        }
    }
    val preferences: @Composable () -> Unit = {
        // 3. Preferences: Currency
        SettingsSection("PREFERENCES") {
            SettingsRow(
                icon = Icons.Filled.AttachMoney,
                iconColor = SettingsColors.green,
                title = "Currency",
                value = currentCurrency?.let { "${it.displayName} ${it.symbol}" } ?: "USD",
                trailing = SettingsTrailing.Chevron,
                onClick = onOpenCurrencyPicker,
            )
            if (container.authRepository.hasPasswordLogin) {
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Key,
                    iconColor = colors.gradientStart,
                    title = "Change Password",
                    trailing = SettingsTrailing.Chevron,
                    onClick = onOpenChangePassword,
                )
            }
        }
    }
    val notifications: @Composable () -> Unit = {
        // 4. Notifications
        SettingsSection("NOTIFICATIONS") {
            SettingsRow(icon = Icons.Filled.Notifications, iconColor = SettingsColors.red, title = "Push Notifications") {
                // Without the 48dp minimum touch box and slightly scaled down,
                // this row is the same height as the others (like iOS's toggle).
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Switch(
                    modifier = Modifier.height(24.dp).scale(0.8f),
                    checked = notificationsEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsEnabled = true
                            }
                        } else {
                            showNotificationsBlockedDialog = true
                        }
                    },
                )
                }
            }
        }
    }
    val subscription: @Composable () -> Unit = {
        // 5. Subscription
        SettingsSection("SUBSCRIPTION") {
            val isPro = landlord?.isPro == true
            SettingsRow(
                icon = Icons.Filled.WorkspacePremium,
                iconColor = if (isPro) colors.gold else SettingsColors.grey,
                title = if (isPro) "Maskan Pro" else "Free Plan",
                subtitle = if (isPro) "All features unlocked" else "1 property · 1 co-owner per building",
            ) {
                if (isPro) StatusBadge(text = "Active", color = colors.success)
            }
            SettingsDivider()
            if (isPro) {
                SettingsRow(
                    icon = Icons.Filled.Autorenew,
                    iconColor = SettingsColors.blue,
                    title = "Manage Subscription",
                    trailing = SettingsTrailing.External,
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")))
                        }
                    },
                )
            } else {
                SettingsRow(
                    icon = Icons.Filled.ArrowCircleUp,
                    iconColor = colors.gradientStart,
                    title = "Upgrade to Pro",
                    titleColor = colors.gradientStart,
                    titleWeight = FontWeight.SemiBold,
                    trailing = SettingsTrailing.Chevron,
                    onClick = onOpenPaywall,
                )
            }
        }
    }
    val about: @Composable () -> Unit = {
        // 6. About
        SettingsSection("ABOUT") {
            SettingsRow(
                icon = Icons.Filled.Email,
                iconColor = SettingsColors.orange,
                title = "Contact Support",
                trailing = SettingsTrailing.External,
                onClick = { openSupportEmail(context) },
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Filled.Star,
                iconColor = SettingsColors.yellow,
                title = "Rate Maskan",
                trailing = SettingsTrailing.External,
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                    }
                },
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Filled.PanTool,
                iconColor = SettingsColors.blue,
                title = "Privacy Policy",
                trailing = SettingsTrailing.Chevron,
                onClick = { openPrivacyPolicy(context) },
            )
        }
    }
    val logOut: @Composable () -> Unit = {
        // 7. Log Out
        MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                iconColor = colors.danger,
                title = "Log Out",
                titleColor = colors.danger,
                titleWeight = FontWeight.SemiBold,
                onClick = { showLogoutDialog = true },
            )
        }
    }
    val deleteAccount: @Composable () -> Unit = {
        Text(
            text = if (isDeletingAccount) "Deleting…" else "Delete Account",
            style = MaskanType.secondary,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isDeletingAccount) { showDeleteAccountDialog = true }
                .padding(vertical = 8.dp),
        )
    }
    val version: @Composable () -> Unit = {
        Text(
            text = "App version $versionName",
            style = MaskanType.caption,
            color = colors.textTertiary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        if (expanded) {
            SettingsSkyline(modifier = Modifier.align(Alignment.BottomCenter))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaskanDimens.screenHPadding, vertical = 24.dp)
                .padding(bottom = LocalLandlordContentBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Text(text = "Settings", style = MaskanType.screenTitle, color = colors.textPrimary)

            if (expanded) {
                // Tablet: account on the left, sections in two columns (iPad layout).
                SettingsTabletLayout(
                    account = {
                        header()
                        PlanBadge(isPro = landlord?.isPro == true)
                    },
                    accountFooter = {
                        logOut()
                        deleteAccount()
                        version()
                    },
                    leading = {
                        appearance()
                        preferences()
                        notifications()
                    },
                    trailing = {
                        subscription()
                        about()
                    },
                )
            } else {
                header()
                appearance()
                preferences()
                notifications()
                subscription()
                about()
                logOut()
                deleteAccount()
                version()
            }
        }
    }

    if (showNotificationsBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsBlockedDialog = false },
            title = { Text("Notifications Blocked") },
            text = { Text("To change notification permissions, go to Settings > Maskan.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationsBlockedDialog = false
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { showNotificationsBlockedDialog = false }) { Text("Cancel") } },
        )
    }


    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("You will be signed out of your account.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut()
                    onSignedOut()
                }) { Text("Log Out", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account?") },
            text = {
                Text(
                    "Your account and all its data — properties, tenants, bills, and documents — will be permanently deleted. This can't be undone.\n\n" +
                        "Active subscriptions aren't cancelled automatically — manage them in Google Play › Payments & subscriptions.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    showDeleteAccountFinalDialog = true
                }) { Text("Delete Account", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") } },
        )
    }

    if (showDeleteAccountFinalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountFinalDialog = false },
            title = { Text("Are you absolutely sure?") },
            text = { Text("Your account, properties, tenants, bills, and documents will be deleted right away and can't be recovered.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountFinalDialog = false
                    isDeletingAccount = true
                    scope.launch {
                        try {
                            viewModel.authRepository.deleteAccount()
                            viewModel.signOut()
                            onSignedOut()
                        } catch (t: Throwable) {
                            deleteAccountError = t.message ?: "Something went wrong."
                        } finally {
                            isDeletingAccount = false
                        }
                    }
                }) { Text("Delete Permanently", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountFinalDialog = false }) { Text("Cancel") } },
        )
    }

    deleteAccountError?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteAccountError = null },
            title = { Text("Couldn't Delete Account") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { deleteAccountError = null }) { Text("OK") } },
        )
    }
}

private const val SUPPORT_EMAIL = "studiodxbai@gmail.com"

/** Same hosted policy the iOS tenant app shows. */
internal const val PRIVACY_POLICY_URL = "https://dxbaistudio.com/app-privacy-policy"

internal fun openPrivacyPolicy(context: android.content.Context) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
}

private fun openSupportEmail(context: android.content.Context) {
    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))) }
}

// MARK: - iOS-style settings rows (LandlordProfileView's settingsSection / settingsIcon)

/** System-style tile colours used by the iOS settings icons. */
private object SettingsColors {
    val purple = Color(0xFFAF52DE)
    val green = Color(0xFF34C759)
    val red = Color(0xFFFF3B30)
    val orange = Color(0xFFFF9500)
    val yellow = Color(0xFFFFBF00)
    val blue = Color(0xFF007AFF)
    val grey = Color(0xFF8E8E93)
}

private enum class SettingsTrailing { None, Chevron, External }

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaskanTheme.colors
    Column {
        Text(
            text = title,
            style = MaskanType.overline,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), content = content)
    }
}

/** Tablet account card: the plan, like the iPad plan badge. */
@Composable
private fun PlanBadge(isPro: Boolean) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isPro) colors.primaryGradient else SolidColor(colors.textSecondary.copy(alpha = 0.12f)))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (isPro) Icons.Filled.WorkspacePremium else Icons.Filled.Person,
            contentDescription = null,
            tint = if (isPro) Color.White else colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = if (isPro) "Maskan Pro" else "Free Plan",
            style = MaskanType.caption.copy(fontWeight = FontWeight.SemiBold),
            color = if (isPro) Color.White else colors.textSecondary,
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaskanTheme.colors.border, modifier = Modifier.padding(start = 58.dp))
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = MaskanTheme.colors.textPrimary,
    titleWeight: FontWeight = FontWeight.Medium,
    trailing: SettingsTrailing = SettingsTrailing.None,
    onClick: (() -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp).background(iconColor, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaskanType.bodyMedium.copy(fontWeight = titleWeight), color = titleColor)
            subtitle?.let { Text(text = it, style = MaskanType.caption.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary) }
        }
        value?.let { Text(text = it, style = MaskanType.secondary.copy(fontSize = 13.sp), color = colors.textSecondary, maxLines = 1) }
        accessory?.invoke()
        when (trailing) {
            SettingsTrailing.Chevron -> Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
            SettingsTrailing.External -> Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
            SettingsTrailing.None -> Unit
        }
    }
}

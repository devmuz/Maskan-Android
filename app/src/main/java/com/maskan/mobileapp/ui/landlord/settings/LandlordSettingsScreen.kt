package com.maskan.mobileapp.ui.landlord.settings

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
    onSignedOut: () -> Unit,
) {
    val container = LocalAppContainer.current
    val colors = MaskanTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val email = container.authRepository.currentUser?.email ?: "Landlord"
    val landlord by viewModel.landlord.collectAsStateWithLifecycle()
    val theme by container.themePreferences.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val currentCurrency = remember(landlord?.currencyCode) { Currencies.byCode(landlord?.currencyCode) }

    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var showNotificationsBlockedDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        if (!granted) showNotificationsBlockedDialog = true
    }
    LaunchedEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaskanDimens.screenHPadding, vertical = 24.dp)
                .padding(bottom = LocalLandlordContentBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            Text(text = "Settings", style = MaskanType.screenTitle, color = colors.textPrimary)

            // 1. Header
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

            // 2. Appearance
            Column {
                Text(text = "APPEARANCE", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                Text(text = "Theme", style = MaskanType.fieldLabel, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                SegmentedControl(
                    options = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK),
                    selected = theme,
                    onSelect = { scope.launch { container.themePreferences.setTheme(it) } },
                    label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                )
            }

            // 3. Preferences: Currency
            Column {
                Text(text = "PREFERENCES", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCurrencyPicker),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Currency", style = MaskanType.body, color = colors.textPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = currentCurrency?.let { "${it.displayName} ${it.symbol}" } ?: "USD",
                                style = MaskanType.bodyMedium,
                                color = colors.textSecondary,
                            )
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                        }
                    }
                }
            }

            // 4. Notifications
            Column {
                Text(text = "NOTIFICATIONS", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Push Notifications", style = MaskanType.body, color = colors.textPrimary)
                        Switch(
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

            // 5. Subscription
            Column {
                Text(text = "SUBSCRIPTION", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    val isPro = landlord?.isPro == true
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background((if (isPro) colors.gold else colors.textTertiary).copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = if (isPro) colors.gold else colors.textTertiary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if (isPro) "Maskan Pro" else "Free Plan", style = MaskanType.bodyMedium, color = colors.textPrimary)
                            Text(
                                text = if (isPro) "All features unlocked" else "1 property · 1 co-owner per building",
                                style = MaskanType.secondary,
                                color = colors.textSecondary,
                            )
                        }
                        if (isPro) StatusBadge(text = "Active", color = colors.success)
                    }

                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 12.dp))

                    if (isPro) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")))
                                }
                            },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Manage Subscription", style = MaskanType.body, color = colors.textPrimary)
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPaywall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Upgrade to Pro", style = MaskanType.bodyMedium, color = colors.gradientStart)
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.gradientStart)
                        }
                    }
                }
            }

            // 6. About
            Column {
                Text(text = "ABOUT", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                            }
                        }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Rate Maskan", style = MaskanType.body, color = colors.textPrimary)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showPrivacyDialog = true }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Privacy Policy", style = MaskanType.body, color = colors.textPrimary)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Version", style = MaskanType.body, color = colors.textPrimary)
                        Text(text = versionName, style = MaskanType.secondary, color = colors.textSecondary)
                    }
                }
            }

            // 7. Log Out
            GradientButton(
                text = "Log Out",
                onClick = { showLogoutDialog = true },
                brush = SolidColor(colors.fieldBackground),
                contentColor = colors.danger,
            )

            Text(
                text = "Maskan $versionName",
                style = MaskanType.caption,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showNotificationsBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsBlockedDialog = false },
            title = { Text("Notifications are off") },
            text = { Text("Turn on notifications in system settings to receive alerts from Maskan.") },
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

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = { Text("The Maskan privacy policy will open here once a hosted URL is published.") },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") } },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
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
}

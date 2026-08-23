package com.maskan.mobileapp.ui.tenant

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.prefs.AppTheme
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.SegmentedControl
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import com.maskan.mobileapp.di.LocalAppContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Teal used only for the "Move-in Date" badge — no theme token is close enough. */
private val MoveInDateBadgeColor = Color(0xFF14B8A6)

/** Pushed via the Home toolbar's gear icon (09-tenant-app.md). */
@Composable
fun TenantSettingsScreen(
    viewModel: TenantSessionViewModel,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val colors = MaskanTheme.colors
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tenant by viewModel.tenant.collectAsStateWithLifecycle()
    val theme by container.themePreferences.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

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
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .shadow(4.dp, CircleShape, ambientColor = colors.shadowColor, spotColor = colors.shadowColor)
                    .background(colors.surface, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
            Text(text = "Settings", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            // 1. Header
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val initial = tenant?.name?.trim()?.firstOrNull()?.uppercaseChar()
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(16.dp, CircleShape, ambientColor = colors.indigoDeep, spotColor = colors.indigoDeep)
                        .background(colors.primaryGradient, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (initial != null) {
                        Text(text = initial.toString(), style = MaskanType.screenTitle, color = Color.White)
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }
                Text(text = tenant?.name?.takeIf { it.isNotBlank() } ?: "Tenant", style = MaskanType.cardTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                tenant?.contact?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // 2. Property section
            tenant?.let { t ->
                val propertyRows = buildList {
                    add(Triple(Icons.Filled.Tag as ImageVector, colors.gradientStart, "Property ID" to t.propertyIdCode))
                    t.moveInDate?.let { add(Triple(Icons.Filled.CalendarMonth as ImageVector, MoveInDateBadgeColor, "Move-in Date" to dateFormat.format(it))) }
                    t.rentDueDay?.let { add(Triple(Icons.Filled.Schedule as ImageVector, colors.warning, "Rent Due" to "Day $it of each month")) }
                }
                Column {
                    Text(text = "PROPERTY", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    MaskanCard(modifier = Modifier.fillMaxWidth()) {
                        propertyRows.forEachIndexed { index, (icon, badgeColor, labelValue) ->
                            if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                            SettingsIconRow(icon = icon, badgeColor = badgeColor, label = labelValue.first, value = labelValue.second)
                        }
                    }
                }
            }

            // 3. Appearance
            Column {
                Text(text = "APPEARANCE", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        SettingsBadge(icon = Icons.Filled.Palette, color = colors.gradientEnd)
                        Text(text = "Theme", style = MaskanType.bodyMedium, color = colors.textPrimary)
                    }
                    SegmentedControl(
                        options = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK),
                        selected = theme,
                        onSelect = { scope.launch { container.themePreferences.setTheme(it) } },
                        label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    )
                }
            }

            // 4. Notifications
            Column {
                Text(text = "NOTIFICATIONS", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsBadge(icon = Icons.Filled.Notifications, color = colors.danger)
                            Text(text = "Push Notifications", style = MaskanType.bodyMedium, color = colors.textPrimary)
                        }
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

            // 5. Security
            Column {
                Text(text = "SECURITY", style = MaskanType.overline, color = colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onChangePassword),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsBadge(icon = Icons.Filled.VpnKey, color = colors.warning)
                            Text(text = "Change Password", style = MaskanType.bodyMedium, color = colors.textPrimary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
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
                        }.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsBadge(icon = Icons.Filled.Star, color = colors.gold)
                            Text(text = "Rate Maskan", style = MaskanType.bodyMedium, color = colors.textPrimary)
                        }
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showPrivacyDialog = true }.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsBadge(icon = Icons.Filled.PrivacyTip, color = colors.textTertiary)
                            Text(text = "Privacy Policy", style = MaskanType.bodyMedium, color = colors.textPrimary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                    }
                    HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Version", style = MaskanType.body, color = colors.textPrimary)
                        Text(text = versionName, style = MaskanType.secondary, color = colors.textSecondary)
                    }
                }
            }

            // 7. Log Out
            GradientButton(
                text = "Log Out",
                onClick = { showLogoutDialog = true },
                brush = androidx.compose.ui.graphics.SolidColor(colors.fieldBackground),
                contentColor = colors.danger,
            )

            Box(modifier = Modifier.padding(bottom = 24.dp))
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

@Composable
private fun SettingsIconRow(icon: ImageVector, badgeColor: Color, label: String, value: String) {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsBadge(icon = icon, color = badgeColor)
        Column {
            Text(text = label, style = MaskanType.caption, color = colors.textSecondary)
            Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun SettingsBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier.size(36.dp).background(color, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

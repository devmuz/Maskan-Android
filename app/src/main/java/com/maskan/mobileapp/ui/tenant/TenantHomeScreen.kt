package com.maskan.mobileapp.ui.tenant

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.maskan.mobileapp.data.util.AmountFormatter
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Locale

/** Home (09-tenant-app.md) — the tenant shell's single root screen, no tab bar. */
@Composable
fun TenantHomeScreen(
    viewModel: TenantSessionViewModel,
    onAddRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRequests: () -> Unit,
    onBillClick: (String) -> Unit,
) {
    val colors = MaskanTheme.colors
    val activity = LocalContext.current as? Activity
    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler { showExitConfirm = true }

    val tenant by viewModel.tenant.collectAsStateWithLifecycle()
    val property by viewModel.property.collectAsStateWithLifecycle()
    val pendingBills by viewModel.pendingBills.collectAsStateWithLifecycle()
    val paidBills by viewModel.paidBills.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val totalDue by viewModel.totalDue.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    val title = tenant?.name?.trim()?.takeIf { it.isNotBlank() }?.split(" ")?.firstOrNull()
        ?.let { "Hello, $it" } ?: "Hi!"

    val nothingAtAll = pendingBills.isEmpty() && paidBills.isEmpty() && requests.isEmpty()

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRequest, containerColor = colors.gradientStart, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "New request")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(colors.background)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp).padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaskanType.screenTitle, color = colors.textPrimary)
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = colors.textPrimary, modifier = Modifier.size(28.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
                verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
            ) {
                // 1. Hero card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primaryGradient, RoundedCornerShape(MaskanDimens.cornerRadius))
                        .padding(MaskanDimens.cardPadding),
                ) {
                    Text(
                        text = if (totalDue > 0) AmountFormatter.format(totalDue, currencyCode) else "$currencyCode 0.00",
                        style = MaskanType.screenTitle,
                        color = Color.White,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (pendingBills.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.height(16.dp),
                        )
                        Text(
                            text = if (pendingBills.isEmpty()) "No pending dues" else "${pendingBills.size} bill(s) outstanding",
                            style = MaskanType.secondary,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }

                // 2. Property card
                if (property != null) {
                    val prop = property!!
                    MaskanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                        if (!prop.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = prop.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(topStart = MaskanDimens.cornerRadius, topEnd = MaskanDimens.cornerRadius)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Column(modifier = Modifier.padding(MaskanDimens.cardPadding)) {
                            Text(
                                text = if (prop.unit.isNotBlank()) "${prop.displayBuildingName} · ${prop.unit}" else prop.displayBuildingName,
                                style = MaskanType.cardTitle,
                                color = colors.textPrimary,
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.height(14.dp))
                                Text(text = prop.address, style = MaskanType.secondary, color = colors.textSecondary)
                            }
                            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 12.dp))
                            val metrics = buildList {
                                add(Triple(Icons.Filled.Payments as ImageVector, "Monthly Rent", AmountFormatter.format(prop.monthlyRent, currencyCode)))
                                add(Triple(Icons.Filled.Tag as ImageVector, "Property ID", prop.propertyIdCode))
                                prop.propertyType?.let { add(Triple(Icons.Filled.Apartment as ImageVector, "Type", it.label)) }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                metrics.forEachIndexed { index, (icon, label, value) ->
                                    MetricCell(icon = icon, label = label, value = value, modifier = Modifier.weight(1f))
                                    if (index < metrics.lastIndex) {
                                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(colors.border))
                                    }
                                }
                            }
                        }
                    }
                } else if (tenant != null) {
                    MaskanCard(modifier = Modifier.fillMaxWidth(), subtle = true) {
                        Text(text = tenant!!.propertyIdCode, style = MaskanType.bodyMedium, color = colors.textPrimary)
                        tenant!!.moveInDate?.let {
                            Text(text = "Since ${dateFormat.format(it)}", style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }

                if (nothingAtAll) {
                    EmptyState(
                        icon = Icons.Filled.CheckCircle,
                        title = "All caught up",
                        message = "No pending bills or dues at the moment.",
                    )
                } else {
                    // 3. Outstanding Bills
                    if (pendingBills.isNotEmpty()) {
                        MaskanCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Outstanding Bills", style = MaskanType.cardTitle, color = colors.textPrimary)
                                StatusBadge(text = "${pendingBills.size}", color = colors.danger)
                            }
                            pendingBills.forEach { bill ->
                                BillRow(bill = bill, currencyCode = currencyCode, onClick = { onBillClick(bill.id) })
                            }
                        }
                    }

                    // 4. Recent Payments
                    if (paidBills.isNotEmpty()) {
                        MaskanCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "RECENT PAYMENTS", style = MaskanType.overline, color = colors.textSecondary)
                                if (paidBills.size > 5) {
                                    Text(
                                        text = "View All",
                                        style = MaskanType.bodyMedium,
                                        color = colors.gradientStart,
                                        modifier = Modifier.clickableNoRipple(onOpenHistory),
                                    )
                                }
                            }
                            paidBills.take(5).forEach { bill ->
                                BillRow(bill = bill, currencyCode = currencyCode, onClick = { onBillClick(bill.id) })
                            }
                        }
                    }

                    // 5. My Requests
                    if (requests.isNotEmpty()) {
                        MaskanCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "MY REQUESTS", style = MaskanType.overline, color = colors.textSecondary)
                                if (requests.size > 3) {
                                    Text(
                                        text = "View All",
                                        style = MaskanType.bodyMedium,
                                        color = colors.gradientStart,
                                        modifier = Modifier.clickableNoRipple(onOpenRequests),
                                    )
                                }
                            }
                            requests.take(3).forEach { request -> RequestRow(request = request) }
                        }
                    }
                }

                // Extra bottom spacing so content doesn't sit under the FAB.
                Column(modifier = Modifier.padding(bottom = 72.dp)) {}
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit Maskan?") },
            text = { Text("Are you sure you want to exit the app?") },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) { Text("Exit", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MetricCell(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(32.dp).background(colors.gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
        Text(text = label, style = MaskanType.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)

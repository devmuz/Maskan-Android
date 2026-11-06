package com.maskan.mobileapp.ui.landlord.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.FilterChipBar
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.landlord.LocalLandlordContentBottomInset
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TenantFilter { ACTIVE, OLD }

@Composable
/**
 * [selectedTenantId] / [onVisibleTenantIds]: tablet list/detail split — outlines
 * the tenant shown beside the list and reports the list order so the first
 * one can be selected by default.
 */
fun TenantsScreen(
    viewModel: LandlordViewModel,
    onTenantClick: (String) -> Unit,
    onAddClick: () -> Unit,
    selectedTenantId: String? = null,
    onVisibleTenantIds: ((List<String>) -> Unit)? = null,
) {
    val colors = MaskanTheme.colors
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val propertiesById = remember(properties) { properties.associateBy { it.id } }
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(TenantFilter.ACTIVE) }
    val hasOldTenants = tenants.any { !it.isDeleted && !it.isActive }
    LaunchedEffect(hasOldTenants) {
        if (!hasOldTenants) filter = TenantFilter.ACTIVE
    }
    val filtered = remember(tenants, filter) {
        // Soft-deleted tenants are excluded from both tabs — deleted != old (feature-tenants.md).
        tenants.filter { !it.isDeleted && if (filter == TenantFilter.ACTIVE) it.isActive else !it.isActive }
    }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    if (onVisibleTenantIds != null) {
        LaunchedEffect(filtered) { onVisibleTenantIds(filtered.map { it.id }) }
    }

    var tenantForRemoval by remember { mutableStateOf<Tenant?>(null) }
    var removalError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaskanDimens.screenHPadding,
                end = MaskanDimens.screenHPadding,
                top = 16.dp,
                bottom = LocalLandlordContentBottomInset.current,
            ),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing),
        ) {
            item { Text(text = "Tenants", style = MaskanType.screenTitle, color = colors.textPrimary) }
            // Like iOS, the Active/Old switch only appears once there are old tenants.
            if (hasOldTenants) {
                item {
                    FilterChipBar(
                        options = listOf(TenantFilter.ACTIVE, TenantFilter.OLD),
                        selected = filter,
                        onSelect = { filter = it },
                        label = { if (it == TenantFilter.ACTIVE) "Active" else "Old" },
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.People,
                        title = if (filter == TenantFilter.ACTIVE) "No active tenants" else "No old tenants",
                        message = if (filter == TenantFilter.ACTIVE) {
                            "Assign a tenant to one of your vacant properties to get started."
                        } else {
                            "Tenants you mark as moved out will appear here."
                        },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { tenant ->
                    TenantRow(
                        tenant = tenant,
                        property = propertiesById[tenant.resolvedPropertyId],
                        dateFormat = dateFormat,
                        selected = tenant.id == selectedTenantId,
                        onClick = { onTenantClick(tenant.id) },
                        onEdit = { onTenantClick(tenant.id) },
                        onRemove = { tenantForRemoval = tenant },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = colors.gradientStart,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .padding(bottom = LocalLandlordContentBottomInset.current),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Assign Tenant")
        }
    }

    tenantForRemoval?.let { tenant ->
        // Active tenants stage through "Old" first (moveOutDate = today); only an
        // already-old tenant is permanently (soft-)removed.
        AlertDialog(
            onDismissRequest = { tenantForRemoval = null },
            title = { Text(if (tenant.isActive) "Mark tenant as moved out?" else "Remove tenant?") },
            text = {
                Text(
                    if (tenant.isActive) {
                        "${tenant.name} will no longer be able to log in, and ${tenant.propertyIdCode} will be marked vacant. Their record moves to Old Tenants."
                    } else {
                        "${tenant.name} will be permanently removed from the tenant list. Payment history will remain accessible in Bills."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = tenant
                    tenantForRemoval = null
                    scope.launch {
                        try {
                            if (target.isActive) {
                                viewModel.tenantRepository.markMovedOut(target, Date())
                            } else {
                                viewModel.tenantRepository.deleteTenant(target)
                            }
                        } catch (t: Throwable) {
                            removalError = t.message
                        }
                    }
                }) { Text(if (tenant.isActive) "Mark Moved Out" else "Remove", color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { tenantForRemoval = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TenantRow(
    tenant: Tenant,
    property: Property?,
    dateFormat: SimpleDateFormat,
    selected: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaskanTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    MaskanCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.border(2.dp, colors.gradientStart, RoundedCornerShape(MaskanDimens.cornerRadius)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tenant.name, style = MaskanType.cardTitle, color = colors.textPrimary)
                val propertyLabel = property?.let {
                    if (it.unit.isNotBlank()) "${it.unit} · ${it.displayBuildingName}" else it.displayBuildingName
                } ?: ""
                Text(text = propertyLabel, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Box(modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = colors.textSecondary,
                    modifier = Modifier.clickable { menuExpanded = true },
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text(if (tenant.isActive) "Mark Moved Out" else "Remove") },
                        onClick = { menuExpanded = false; onRemove() },
                    )
                }
            }
        }
        // ID and date share one line; each stays on a single line rather than
        // wrapping character-by-character when space is tight.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "# ${tenant.propertyIdCode}",
                style = MaskanType.secondary,
                color = colors.gradientStart,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            val dateLabel = if (tenant.isActive) {
                tenant.moveInDate?.let { "Since ${dateFormat.format(it)}" } ?: ""
            } else {
                tenant.moveOutDate?.let { "Moved out ${dateFormat.format(it)}" } ?: "Moved out"
            }
            Text(
                text = dateLabel,
                style = MaskanType.secondary,
                color = if (tenant.isActive) colors.textSecondary else colors.warning,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

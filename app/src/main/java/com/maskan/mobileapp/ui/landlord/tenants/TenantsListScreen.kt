package com.maskan.mobileapp.ui.landlord.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.Property
import com.maskan.mobileapp.data.model.Tenant
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.SegmentedControl
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private enum class TenantFilter { ACTIVE, OLD }

@Composable
fun TenantsScreen(viewModel: LandlordViewModel, onTenantClick: (String) -> Unit, onAddClick: () -> Unit) {
    val colors = MaskanTheme.colors
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val propertiesById = remember(properties) { properties.associateBy { it.id } }
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(TenantFilter.ACTIVE) }
    val filtered = remember(tenants, filter) {
        tenants.filter { if (filter == TenantFilter.ACTIVE) it.isActive else !it.isActive }
    }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var tenantForRemoval by remember { mutableStateOf<Tenant?>(null) }
    var removalError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing),
        ) {
            item { Text(text = "Tenants", style = MaskanType.screenTitle, color = colors.textPrimary) }
            item {
                SegmentedControl(
                    options = listOf(TenantFilter.ACTIVE, TenantFilter.OLD),
                    selected = filter,
                    onSelect = { filter = it },
                    label = { if (it == TenantFilter.ACTIVE) "Active" else "Old" },
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.People,
                        title = if (filter == TenantFilter.ACTIVE) "No active tenants" else "No old tenants",
                        message = if (filter == TenantFilter.ACTIVE) {
                            "Assign a tenant to one of your vacant properties to get started"
                        } else {
                            "Tenants you mark as moved out will appear here."
                        },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { tenant ->
                    TenantRow(
                        tenant = tenant,
                        property = propertiesById[tenant.propertyId],
                        dateFormat = dateFormat,
                        onClick = { onTenantClick(tenant.id) },
                        onEdit = { onTenantClick(tenant.id) },
                        onRemove = { tenantForRemoval = tenant },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = MaskanDimens.screenHPadding)
                .size(44.dp)
                .background(colors.fieldBackground, CircleShape)
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Assign Tenant", tint = colors.gradientStart)
        }
    }

    tenantForRemoval?.let { tenant ->
        AlertDialog(
            onDismissRequest = { tenantForRemoval = null },
            title = { Text("Remove ${tenant.name}?") },
            text = {
                Text(
                    if (tenant.isActive) {
                        "${tenant.name} will no longer be able to log in, and ${tenant.propertyIdCode} will be marked vacant."
                    } else {
                        "${tenant.name}'s record and history reference will be permanently removed."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = tenant
                    tenantForRemoval = null
                    scope.launch {
                        try {
                            viewModel.tenantRepository.deleteTenant(target)
                        } catch (t: Throwable) {
                            removalError = t.message
                        }
                    }
                }) { Text("Remove", color = colors.danger) }
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
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaskanTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    MaskanCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(text = tenant.name, style = MaskanType.cardTitle, color = colors.textPrimary)
                val propertyLabel = property?.let {
                    if (it.unit.isNotBlank()) "${it.unit} · ${it.displayBuildingName}" else it.displayBuildingName
                } ?: ""
                Text(text = propertyLabel, style = MaskanType.secondary, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "#", style = MaskanType.secondary, color = colors.gradientStart)
                    Text(text = tenant.propertyIdCode, style = MaskanType.secondary, color = colors.gradientStart)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textSecondary,
                        modifier = Modifier.clickable { menuExpanded = true },
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("Remove") }, onClick = { menuExpanded = false; onRemove() })
                    }
                }
                val dateLabel = if (tenant.isActive) {
                    tenant.moveInDate?.let { "Since ${dateFormat.format(it)}" } ?: ""
                } else {
                    tenant.moveOutDate?.let { "Moved out ${dateFormat.format(it)}" } ?: "Moved out"
                }
                Text(
                    text = dateLabel,
                    style = MaskanType.secondary,
                    color = if (tenant.isActive) colors.textSecondary else colors.warning,
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
        }
    }
}

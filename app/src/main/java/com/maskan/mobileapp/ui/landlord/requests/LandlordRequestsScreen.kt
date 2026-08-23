package com.maskan.mobileapp.ui.landlord.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.ServiceRequest
import com.maskan.mobileapp.data.model.ServiceRequestStatus
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.SegmentedControl
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import com.maskan.mobileapp.ui.tenant.requestCategoryIcon
import com.maskan.mobileapp.ui.tenant.serviceRequestStatusColor
import java.text.SimpleDateFormat
import java.util.Locale

private enum class RequestFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
}

/**
 * Landlord-side counterpart to the tenant's Requests screen
 * (04-landlord-dashboard.md's "Toolbar: Service Requests inbox") — every
 * `serviceRequests` doc across all the landlord's properties, with an
 * All/Pending/In Progress/Resolved filter.
 */
@Composable
fun LandlordRequestsScreen(viewModel: LandlordViewModel, onBack: () -> Unit, onRequestClick: (String) -> Unit) {
    val colors = MaskanTheme.colors
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val tenantsById = remember(tenants) { tenants.associateBy { it.id } }
    val propertiesById = remember(properties) { properties.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var filter by remember { mutableStateOf(RequestFilter.ALL) }
    val filtered = remember(requests, filter) {
        when (filter) {
            RequestFilter.ALL -> requests
            RequestFilter.PENDING -> requests.filter { it.status == ServiceRequestStatus.PENDING }
            RequestFilter.IN_PROGRESS -> requests.filter { it.status == ServiceRequestStatus.IN_PROGRESS }
            RequestFilter.RESOLVED -> requests.filter { it.status == ServiceRequestStatus.RESOLVED }
        }.sortedByDescending { it.createdAt }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = MaskanDimens.screenHPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.itemSpacing),
        ) {
            item { Text(text = "Service Requests", style = MaskanType.screenTitle, color = colors.textPrimary) }
            item {
                SegmentedControl(
                    options = RequestFilter.entries,
                    selected = filter,
                    onSelect = { filter = it },
                    label = { it.label },
                )
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.RequestPage,
                        title = if (filter == RequestFilter.ALL) "No requests yet" else "No ${filter.label.lowercase()} requests",
                        message = "Requests submitted by your tenants will appear here.",
                    )
                }
            } else {
                items(filtered, key = { it.id }) { request ->
                    RequestListRow(
                        request = request,
                        tenantName = tenantsById[request.tenantId]?.name ?: "Unknown tenant",
                        propertyName = propertiesById[request.propertyId]?.let {
                            if (it.unit.isNotBlank()) "${it.displayBuildingName} · ${it.unit}" else it.displayBuildingName
                        } ?: "",
                        dateFormat = dateFormat,
                        onClick = { onRequestClick(request.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestListRow(
    request: ServiceRequest,
    tenantName: String,
    propertyName: String,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
) {
    val colors = MaskanTheme.colors
    val statusColor = serviceRequestStatusColor(request.status, colors)

    MaskanCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).background(colors.gradientStart.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(requestCategoryIcon(request.category), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(text = request.title, style = MaskanType.bodyMedium, color = colors.textPrimary)
                    Text(
                        text = "$tenantName · $propertyName",
                        style = MaskanType.secondary,
                        color = colors.textSecondary,
                    )
                    request.createdAt?.let {
                        Text(text = dateFormat.format(it), style = MaskanType.caption, color = colors.textTertiary, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            StatusBadge(text = request.status.label, color = statusColor)
        }
    }
}

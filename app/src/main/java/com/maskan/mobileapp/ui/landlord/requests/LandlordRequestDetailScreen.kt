package com.maskan.mobileapp.ui.landlord.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.model.ServiceRequestStatus
import com.maskan.mobileapp.ui.components.GradientButton
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.landlord.LandlordViewModel
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import com.maskan.mobileapp.ui.tenant.requestCategoryIcon
import com.maskan.mobileapp.ui.tenant.serviceRequestStatusColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Status-transition detail sheet (04-landlord-dashboard.md): Mark In
 * Progress -> Mark as Resolved, or straight to Resolved.
 */
@Composable
fun LandlordRequestDetailScreen(viewModel: LandlordViewModel, requestId: String, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val scope = rememberCoroutineScope()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }

    val request = requests.find { it.id == requestId }

    if (request == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Request not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val tenant = tenants.find { it.id == request.tenantId }
    val property = properties.find { it.id == request.propertyId }
    val statusColor = serviceRequestStatusColor(request.status, colors)

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
            verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
        ) {
            MaskanCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(44.dp).background(colors.gradientStart.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(requestCategoryIcon(request.category), contentDescription = null, tint = colors.gradientStart, modifier = Modifier.size(22.dp))
                    }
                    StatusBadge(text = request.status.label, color = statusColor)
                }
                Text(text = request.title, style = MaskanType.sectionTitle, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                if (request.description.isNotBlank()) {
                    Text(text = request.description, style = MaskanType.body, color = colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
                }

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DetailRow("Category", request.category.label)
                    DetailRow("Tenant", tenant?.name ?: "Unknown tenant")
                    DetailRow(
                        "Property",
                        property?.let { if (it.unit.isNotBlank()) "${it.displayBuildingName} · ${it.unit}" else it.displayBuildingName } ?: "—",
                    )
                    request.createdAt?.let { DetailRow("Submitted", dateFormat.format(it)) }
                    request.resolvedAt?.let { DetailRow("Resolved", dateFormat.format(it)) }
                }
            }

            when (request.status) {
                ServiceRequestStatus.PENDING -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientButton(
                            text = "Mark In Progress",
                            brush = SolidColor(colors.gradientStart),
                            contentColor = Color.White,
                            onClick = { scope.launch { viewModel.serviceRequestRepository.updateStatus(request.id, ServiceRequestStatus.IN_PROGRESS) } },
                        )
                        GradientButton(
                            text = "Mark as Resolved",
                            onClick = { scope.launch { viewModel.serviceRequestRepository.updateStatus(request.id, ServiceRequestStatus.RESOLVED) } },
                        )
                    }
                }
                ServiceRequestStatus.IN_PROGRESS -> {
                    GradientButton(
                        text = "Mark as Resolved",
                        onClick = { scope.launch { viewModel.serviceRequestRepository.updateStatus(request.id, ServiceRequestStatus.RESOLVED) } },
                    )
                }
                ServiceRequestStatus.RESOLVED -> Unit
            }

            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaskanTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaskanType.body, color = colors.textSecondary)
        Text(text = value, style = MaskanType.bodyMedium, color = colors.textPrimary)
    }
}

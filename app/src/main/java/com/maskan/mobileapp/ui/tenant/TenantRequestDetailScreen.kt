package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.components.StatusBadge
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Read-only Request Detail (09-tenant-app.md), from Home or Requests. Reads
 * the live request so a landlord status change shows while it's open.
 */
@Composable
fun TenantRequestDetailScreen(viewModel: TenantSessionViewModel, requestId: String, onBack: () -> Unit) {
    val colors = MaskanTheme.colors
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }

    val request = requests.find { it.id == requestId }

    if (request == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            Text(text = "Request not found", style = MaskanType.body, color = colors.textSecondary)
        }
        return
    }

    val statusColor = serviceRequestStatusColor(request.status, colors)

    Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
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

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    DetailRow("Category", request.category.label)
                    DetailRow("Initiated", request.createdAt?.let { dateFormat.format(it) } ?: "Just now")
                    DetailRow("Completed", request.resolvedAt?.let { dateFormat.format(it) } ?: "Not yet")
                }
            }

            if (request.description.isNotBlank()) {
                MaskanCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "DETAILS", style = MaskanType.overline, color = colors.textSecondary)
                    Text(text = request.description, style = MaskanType.body, color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
                }
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

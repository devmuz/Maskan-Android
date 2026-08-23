package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.components.MaskanCard
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/** Full list, newest first — no status filter, unlike the landlord inbox (09-tenant-app.md). */
@Composable
fun TenantRequestsScreen(viewModel: TenantSessionViewModel, onBack: () -> Unit, onAddRequest: () -> Unit) {
    val colors = MaskanTheme.colors
    val requests by viewModel.requests.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRequest, containerColor = colors.gradientStart, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "New request")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(colors.background)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary) }
            }
            Text(
                text = "Requests",
                style = MaskanType.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = MaskanDimens.screenHPadding, vertical = 8.dp),
            )

            if (requests.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.RequestPage,
                    title = "No requests yet",
                    message = "Tap + to submit a maintenance or service request.",
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = MaskanDimens.screenHPadding),
                    verticalArrangement = Arrangement.spacedBy(MaskanDimens.sectionSpacing),
                ) {
                    MaskanCard(modifier = Modifier.fillMaxWidth()) {
                        requests.forEach { request -> RequestRow(request = request) }
                    }
                    // Extra bottom spacing so content doesn't sit under the FAB.
                    Column(modifier = Modifier.padding(bottom = 72.dp)) {}
                }
            }
        }
    }
}

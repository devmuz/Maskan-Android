package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.components.EmptyState
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType

/**
 * Requests tab — genuinely new scope (09-tenant-app.md): there is no
 * `serviceRequests`-style collection in the schema yet, so this stays an
 * empty-state shell with an unwired "+" action rather than inventing a
 * collection that doesn't exist on the shared backend.
 */
@Composable
fun TenantRequestsScreen(viewModel: TenantSessionViewModel) {
    val colors = MaskanTheme.colors

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: present new service request form once `serviceRequests` collection exists */ }) {
                Icon(Icons.Filled.Add, contentDescription = "New request")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(colors.background)) {
            Text(
                text = "Requests",
                style = MaskanType.screenTitle,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = MaskanDimens.screenHPadding, vertical = 16.dp),
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.RequestPage,
                    title = "No requests yet",
                    message = "Submit a service request and track its status here.",
                )
            }
        }
    }
}

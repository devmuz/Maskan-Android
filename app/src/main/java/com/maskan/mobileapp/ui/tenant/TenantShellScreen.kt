package com.maskan.mobileapp.ui.tenant

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.maskan.mobileapp.di.LocalAppContainer

private object TenantTab {
    const val HOME = "tenant_home"
    const val HISTORY = "tenant_history"
    const val REQUESTS = "tenant_requests"
    const val SETTINGS = "tenant_settings"
}

private data class TenantTabItem(val route: String, val label: String, val icon: ImageVector)

private val tenantTabItems = listOf(
    TenantTabItem(TenantTab.HOME, "Home", Icons.Filled.Home),
    TenantTabItem(TenantTab.HISTORY, "History", Icons.Filled.History),
    TenantTabItem(TenantTab.REQUESTS, "Requests", Icons.Filled.RequestPage),
    TenantTabItem(TenantTab.SETTINGS, "Settings", Icons.Filled.Person),
)

@Composable
fun TenantShellScreen(onSignedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TenantSessionViewModel = viewModel(factory = TenantSessionViewModel.Factory(container))
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tenantTabItems.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TenantTab.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TenantTab.HOME) { TenantHomeScreen(viewModel) }
            composable(TenantTab.HISTORY) { TenantHistoryScreen(viewModel) }
            composable(TenantTab.REQUESTS) { TenantRequestsScreen(viewModel) }
            composable(TenantTab.SETTINGS) { TenantSettingsScreen(viewModel, onSignedOut) }
        }
    }
}

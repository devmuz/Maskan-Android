package com.maskan.mobileapp.ui.landlord

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.landlord.bills.AddBillScreen
import com.maskan.mobileapp.ui.landlord.bills.BillDetailScreen
import com.maskan.mobileapp.ui.landlord.bills.BillsScreen
import com.maskan.mobileapp.ui.landlord.dashboard.DashboardScreen
import com.maskan.mobileapp.ui.landlord.properties.AddPropertyScreen
import com.maskan.mobileapp.ui.landlord.properties.EditPropertyScreen
import com.maskan.mobileapp.ui.landlord.properties.PropertiesScreen
import com.maskan.mobileapp.ui.landlord.properties.PropertyDetailScreen
import com.maskan.mobileapp.ui.landlord.settings.LandlordSettingsScreen
import com.maskan.mobileapp.ui.landlord.tenants.AssignTenantScreen
import com.maskan.mobileapp.ui.landlord.tenants.EditTenantScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantDetailScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantsScreen

private object LandlordTab {
    const val DASHBOARD = "dashboard"
    const val PROPERTIES = "properties"
    const val TENANTS = "tenants"
    const val BILLS = "bills"
    const val SETTINGS = "settings"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    TabItem(LandlordTab.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    TabItem(LandlordTab.PROPERTIES, "Properties", Icons.Filled.Apartment),
    TabItem(LandlordTab.TENANTS, "Tenants", Icons.Filled.People),
    TabItem(LandlordTab.BILLS, "Bills", Icons.Filled.Description),
    TabItem(LandlordTab.SETTINGS, "Settings", Icons.Filled.Person),
)

@Composable
fun LandlordShellScreen(onSignedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LandlordViewModel = viewModel(factory = LandlordViewModel.Factory(container))
    val innerNavController = rememberNavController()

    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabItems.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                innerNavController.navigate(tab.route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = LandlordTab.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LandlordTab.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onAddProperty = { innerNavController.navigate("add_property") },
                    onAddTenant = { innerNavController.navigate("assign_tenant") },
                    onAddBill = { innerNavController.navigate("add_bill") },
                )
            }
            composable(LandlordTab.PROPERTIES) {
                PropertiesScreen(
                    viewModel = viewModel,
                    onPropertyClick = { innerNavController.navigate("property_detail/$it") },
                    onAddClick = { innerNavController.navigate("add_property") },
                )
            }
            composable("property_detail/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                PropertyDetailScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onEdit = { innerNavController.navigate("edit_property/$it") },
                )
            }
            composable("add_property") {
                AddPropertyScreen(
                    viewModel = viewModel,
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }
            composable("edit_property/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                EditPropertyScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }

            composable(LandlordTab.TENANTS) {
                TenantsScreen(
                    viewModel = viewModel,
                    onTenantClick = { innerNavController.navigate("tenant_detail/$it") },
                    onAddClick = { innerNavController.navigate("assign_tenant") },
                )
            }
            composable("tenant_detail/{tenantId}", arguments = listOf(navArgument("tenantId") { type = NavType.StringType })) { entry ->
                TenantDetailScreen(
                    viewModel = viewModel,
                    tenantId = entry.arguments?.getString("tenantId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onEdit = { innerNavController.navigate("edit_tenant/$it") },
                )
            }
            composable("assign_tenant") {
                AssignTenantScreen(
                    viewModel = viewModel,
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }
            composable("edit_tenant/{tenantId}", arguments = listOf(navArgument("tenantId") { type = NavType.StringType })) { entry ->
                EditTenantScreen(
                    viewModel = viewModel,
                    tenantId = entry.arguments?.getString("tenantId").orEmpty(),
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }

            composable(LandlordTab.BILLS) {
                BillsScreen(
                    viewModel = viewModel,
                    onBillClick = { innerNavController.navigate("bill_detail/$it") },
                    onAddClick = { innerNavController.navigate("add_bill") },
                )
            }
            composable("add_bill") {
                AddBillScreen(
                    viewModel = viewModel,
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }
            composable("bill_detail/{billId}", arguments = listOf(navArgument("billId") { type = NavType.StringType })) { entry ->
                BillDetailScreen(
                    viewModel = viewModel,
                    billId = entry.arguments?.getString("billId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                )
            }

            composable(LandlordTab.SETTINGS) {
                LandlordSettingsScreen(viewModel = viewModel, onSignedOut = onSignedOut)
            }
        }
    }
}

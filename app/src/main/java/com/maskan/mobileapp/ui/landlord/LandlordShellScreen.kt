package com.maskan.mobileapp.ui.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maskan.mobileapp.ui.theme.MaskanDimens
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
import com.maskan.mobileapp.ui.landlord.bills.RecordPaymentScreen
import com.maskan.mobileapp.ui.landlord.dashboard.DashboardScreen
import com.maskan.mobileapp.ui.landlord.properties.AddPropertyScreen
import com.maskan.mobileapp.ui.landlord.properties.ArchivedPropertyDetailScreen
import com.maskan.mobileapp.ui.landlord.properties.BuildingDetailScreen
import com.maskan.mobileapp.ui.landlord.properties.EditPropertyScreen
import com.maskan.mobileapp.ui.landlord.properties.PropertiesScreen
import com.maskan.mobileapp.ui.landlord.properties.PropertyDetailScreen
import com.maskan.mobileapp.ui.landlord.requests.LandlordRequestDetailScreen
import com.maskan.mobileapp.ui.landlord.requests.LandlordRequestsScreen
import com.maskan.mobileapp.ui.landlord.settings.CurrencyPickerScreen
import com.maskan.mobileapp.ui.landlord.settings.LandlordSettingsScreen
import com.maskan.mobileapp.ui.landlord.settings.PaywallScreen
import com.maskan.mobileapp.ui.landlord.tenants.AssignTenantScreen
import com.maskan.mobileapp.ui.landlord.tenants.EditTenantScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantDetailScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantPaymentHistoryScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantsScreen

private object LandlordTab {
    const val DASHBOARD = "dashboard"
    const val PROPERTIES = "properties"
    const val TENANTS = "tenants"
    const val BILLS = "bills"
    const val SETTINGS = "settings"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val TabBarShape = RoundedCornerShape(32.dp)

private val tabItems = listOf(
    TabItem(LandlordTab.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    TabItem(LandlordTab.PROPERTIES, "Properties", Icons.Filled.Apartment),
    TabItem(LandlordTab.TENANTS, "Tenants", Icons.Filled.People),
    TabItem(LandlordTab.BILLS, "Bills", Icons.Filled.Description),
    TabItem(LandlordTab.SETTINGS, "Settings", Icons.Filled.Person),
)

/**
 * Extra bottom clearance tab-root screens (Dashboard/Properties/Tenants/Bills/Settings)
 * must reserve in their scrollable content so the last item can clear the floating
 * tab bar. 0.dp on any screen where the bar isn't shown (detail/add/edit routes).
 */
val LocalLandlordContentBottomInset = compositionLocalOf { 0.dp }

@Composable
fun LandlordShellScreen(onSignedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LandlordViewModel = viewModel(factory = LandlordViewModel.Factory(container))
    val innerNavController = rememberNavController()

    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabItems.any { it.route == currentRoute }

    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomInset = navBarInset + MaskanDimens.glassBarContentClearance

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalLandlordContentBottomInset provides if (showBottomBar) contentBottomInset else 0.dp) {
            NavHost(
                navController = innerNavController,
                startDestination = LandlordTab.DASHBOARD,
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
            ) {
            composable(LandlordTab.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onAddTenant = { innerNavController.navigate("assign_tenant") },
                    onAddBill = { innerNavController.navigate("add_bill") },
                    onRecordPayment = { innerNavController.navigate("record_payment") },
                    onOpenRequests = { innerNavController.navigate("requests") },
                )
            }
            composable("record_payment") {
                RecordPaymentScreen(
                    viewModel = viewModel,
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }
            composable(
                "record_payment/{propertyId}",
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
            ) { entry ->
                RecordPaymentScreen(
                    viewModel = viewModel,
                    fixedPropertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                )
            }
            composable("requests") {
                LandlordRequestsScreen(
                    viewModel = viewModel,
                    onBack = { innerNavController.popBackStack() },
                    onRequestClick = { innerNavController.navigate("request_detail/$it") },
                )
            }
            composable("request_detail/{requestId}", arguments = listOf(navArgument("requestId") { type = NavType.StringType })) { entry ->
                LandlordRequestDetailScreen(
                    viewModel = viewModel,
                    requestId = entry.arguments?.getString("requestId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                )
            }
            composable(LandlordTab.PROPERTIES) {
                PropertiesScreen(
                    viewModel = viewModel,
                    onPropertyClick = { innerNavController.navigate("property_detail/$it") },
                    onArchivedPropertyClick = { innerNavController.navigate("archived_property_detail/$it") },
                    onBuildingClick = { innerNavController.navigate("building_detail/$it") },
                    onAddClick = { innerNavController.navigate("add_property") },
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
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
            composable("archived_property_detail/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                ArchivedPropertyDetailScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                )
            }
            composable("building_detail/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                BuildingDetailScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onFlatClick = { innerNavController.navigate("property_detail/$it") },
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
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
                    onViewAllPayments = { innerNavController.navigate("tenant_payment_history/$it") },
                    onRecordPayment = { innerNavController.navigate("record_payment/$it") },
                )
            }
            composable(
                "tenant_payment_history/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) { entry ->
                TenantPaymentHistoryScreen(
                    viewModel = viewModel,
                    tenantId = entry.arguments?.getString("tenantId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
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
                LandlordSettingsScreen(
                    viewModel = viewModel,
                    onOpenCurrencyPicker = { innerNavController.navigate("currency_picker") },
                    onOpenPaywall = { innerNavController.navigate("paywall") },
                    onSignedOut = onSignedOut,
                )
            }
            composable("currency_picker") {
                CurrencyPickerScreen(viewModel = viewModel, onBack = { innerNavController.popBackStack() })
            }
            composable("paywall") {
                PaywallScreen(viewModel = viewModel, onBack = { innerNavController.popBackStack() })
            }
            }
        }

        if (showBottomBar) {
            FloatingTabBar(
                backStackEntry = backStackEntry,
                onTabClick = { route ->
                    innerNavController.navigate(route) {
                        popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun FloatingTabBar(
    backStackEntry: NavBackStackEntry?,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaskanTheme.colors
    Row(
        modifier = modifier
            .padding(horizontal = MaskanDimens.glassBarHMargin)
            .navigationBarsPadding()
            .padding(bottom = MaskanDimens.glassBarBottomMargin)
            .fillMaxWidth()
            .height(MaskanDimens.glassBarHeight)
            .shadow(elevation = 12.dp, shape = TabBarShape, ambientColor = colors.shadowColor, spotColor = colors.shadowColor)
            .clip(TabBarShape)
            .background(colors.surface)
            .border(width = 1.dp, color = colors.border, shape = TabBarShape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabItems.forEach { tab ->
            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
            BottomTabItem(
                tab = tab,
                selected = selected,
                onClick = { onTabClick(tab.route) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Material3's [NavigationBarItem] indicator pill only ever wraps the icon, never the
 * label — a spec limitation, not something the color/shape params can override. This
 * builds the pill ourselves so it wraps icon + label together, matching the design.
 */
@Composable
private fun BottomTabItem(tab: TabItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    val tint = if (selected) colors.gradientStart else colors.textTertiary
    Column(
        modifier = modifier
            .padding(vertical = 8.dp)
            .clip(TabBarShape)
            .then(if (selected) Modifier.background(colors.gradientStart.copy(alpha = 0.16f)) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = tab.label,
            style = MaskanType.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

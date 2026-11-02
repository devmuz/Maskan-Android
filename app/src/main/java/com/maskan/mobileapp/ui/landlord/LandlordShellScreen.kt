package com.maskan.mobileapp.ui.landlord

import com.maskan.mobileapp.data.repository.isBuildingLocked
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.maskan.mobileapp.data.model.PropertyType
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.coachmark.CoachMarkAnchorRegistry
import com.maskan.mobileapp.ui.coachmark.CoachMarkOverlay
import com.maskan.mobileapp.ui.coachmark.CoachMarkSequencer
import com.maskan.mobileapp.ui.coachmark.CoachMarkStep
import com.maskan.mobileapp.ui.coachmark.OnboardingStage
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
import com.maskan.mobileapp.ui.landlord.properties.PropertyDocumentsScreen
import com.maskan.mobileapp.ui.landlord.properties.PropertyExpensesScreen
import com.maskan.mobileapp.ui.landlord.requests.LandlordRequestDetailScreen
import com.maskan.mobileapp.ui.landlord.requests.LandlordRequestsScreen
import com.maskan.mobileapp.ui.landlord.settings.CurrencyPickerScreen
import com.maskan.mobileapp.ui.landlord.settings.LandlordSettingsScreen
import com.maskan.mobileapp.ui.landlord.settings.PaywallScreen
import com.maskan.mobileapp.ui.landlord.tenants.AssignTenantScreen
import com.maskan.mobileapp.ui.landlord.tenants.EditTenantScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantDetailScreen
import com.maskan.mobileapp.ui.landlord.tenants.TenantDocumentsScreen
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

    val anchorRegistry = remember { CoachMarkAnchorRegistry() }
    val coachMarkSequencer: CoachMarkSequencer = viewModel(factory = CoachMarkSequencer.Factory(container.coachmarkPreferences))
    val coachMarkStage by coachMarkSequencer.stage.collectAsStateWithLifecycle()
    var dismissedAddButtonHint by remember { mutableStateOf(false) }

    // A landlord who already has a property (however it was added) shouldn't
    // see the "add property" tour — the sequencer only advances via its own
    // 4-step intro UI, so this check against the real data is what actually
    // makes the "seen" flag stick once there's nothing left to onboard.
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    LaunchedEffect(properties.isNotEmpty()) {
        if (properties.isNotEmpty()) {
            coachMarkSequencer.finishIfPropertiesAlreadyExist()
        }
    }

    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabItems.any { it.route == currentRoute }

    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomInset = navBarInset + MaskanDimens.glassBarContentClearance

    // Land on Properties only when the landlord has no properties yet (so the "add your first
    // property" path — and the onboarding Add-button hint — is right there); otherwise stay on
    // Dashboard. Decided once per session, after the property list has really loaded.
    val propertiesLoaded by viewModel.propertiesLoaded.collectAsStateWithLifecycle()
    var initialTabChosen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(propertiesLoaded) {
        if (propertiesLoaded && !initialTabChosen) {
            initialTabChosen = true
            if (properties.isEmpty()) {
                innerNavController.navigate(LandlordTab.PROPERTIES) {
                    popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

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
                    onBillClick = { innerNavController.navigate("bill_detail/$it") },
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
                    anchorRegistry = anchorRegistry,
                    coachMarkSequencer = coachMarkSequencer,
                )
            }
            composable("property_detail/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                PropertyDetailScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onEdit = { innerNavController.navigate("edit_property/$it") },
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
                )
            }
            composable("archived_property_detail/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                ArchivedPropertyDetailScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onDocumentsClick = { innerNavController.navigate("property_documents/$it") },
                    onBillClick = { innerNavController.navigate("bill_detail/$it") },
                )
            }
            composable("building_detail/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                BuildingDetailScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onFlatClick = { innerNavController.navigate("property_detail/$it") },
                    onEdit = { innerNavController.navigate("edit_property/$it") },
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
                    onDocumentsClick = { innerNavController.navigate("property_documents/$it") },
                    onBillClick = { innerNavController.navigate("bill_detail/$it") },
                    onViewAllExpenses = { innerNavController.navigate("property_expenses/$it") },
                )
            }
            composable("property_expenses/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                PropertyExpensesScreen(
                    viewModel = viewModel,
                    propertyId = entry.arguments?.getString("propertyId").orEmpty(),
                    onBack = { innerNavController.popBackStack() },
                    onBillClick = { innerNavController.navigate("bill_detail/$it") },
                )
            }
            composable("property_documents/{propertyId}", arguments = listOf(navArgument("propertyId") { type = NavType.StringType })) { entry ->
                val propertyId = entry.arguments?.getString("propertyId").orEmpty()
                val properties by viewModel.properties.collectAsStateWithLifecycle()
                val landlord by viewModel.landlord.collectAsStateWithLifecycle()
                val locked = properties.find { it.id == propertyId }?.let {
                    isBuildingLocked(it.groupKey, it.landlordId, viewModel.landlordUid, landlord?.isPro == true, properties)
                } == true
                PropertyDocumentsScreen(
                    propertyId = propertyId,
                    onBack = { innerNavController.popBackStack() },
                    locked = locked,
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
                )
            }
            composable("add_property") {
                AddPropertyScreen(
                    viewModel = viewModel,
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                    anchorRegistry = anchorRegistry,
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
                    onDocumentsClick = { innerNavController.navigate("tenant_documents/$it") },
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
                )
            }
            composable("tenant_documents/{tenantId}", arguments = listOf(navArgument("tenantId") { type = NavType.StringType })) { entry ->
                val tenantId = entry.arguments?.getString("tenantId").orEmpty()
                val tenants by viewModel.tenants.collectAsStateWithLifecycle()
                val landlordId = tenants.find { it.id == tenantId }?.landlordId ?: viewModel.landlordUid
                TenantDocumentsScreen(
                    tenantId = tenantId,
                    landlordId = landlordId,
                    onBack = { innerNavController.popBackStack() },
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
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
                )
            }
            composable("edit_tenant/{tenantId}", arguments = listOf(navArgument("tenantId") { type = NavType.StringType })) { entry ->
                EditTenantScreen(
                    viewModel = viewModel,
                    tenantId = entry.arguments?.getString("tenantId").orEmpty(),
                    onDone = { innerNavController.popBackStack() },
                    onCancel = { innerNavController.popBackStack() },
                    onUpgradeRequired = { innerNavController.navigate("paywall") },
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

        if (currentRoute == LandlordTab.PROPERTIES &&
            coachMarkStage == OnboardingStage.PointAtAddButton &&
            !dismissedAddButtonHint
        ) {
            CoachMarkOverlay(
                step = CoachMarkStep(
                    id = "pointAtAddButton",
                    description = "Add Properties to start tracking rent, bills, and tenants all in one place. Tap the + button to get started.",
                ),
                stepNumber = 1,
                totalSteps = 1,
                targetRect = anchorRegistry["addProperty"],
                canGoPrevious = false,
                isLastStep = true,
                showsStepCounter = false,
                showsSkip = false,
                // "Done" only dismisses the hint locally — it must not finish the whole
                // sequencer, or the 4 property-type steps would never show once the user
                // actually taps the real + button (same reasoning as iOS).
                onNext = { dismissedAddButtonHint = true },
                onPrevious = {},
                onSkip = coachMarkSequencer::skip,
            )
        }

        if (currentRoute == "add_property") {
            val propertyTypeStage = coachMarkStage as? OnboardingStage.PropertyTypeIntro
            if (propertyTypeStage != null) {
                val type = PropertyType.entries[propertyTypeStage.step]
                CoachMarkOverlay(
                    step = CoachMarkStep(id = type.name, description = propertyTypeDescription(type)),
                    stepNumber = propertyTypeStage.step + 1,
                    totalSteps = PropertyType.entries.size,
                    targetRect = anchorRegistry[type.name],
                    canGoPrevious = propertyTypeStage.step > 0,
                    isLastStep = propertyTypeStage.step == PropertyType.entries.lastIndex,
                    onNext = coachMarkSequencer::advancePropertyTypeIntro,
                    onPrevious = coachMarkSequencer::stepBackPropertyTypeIntro,
                    onSkip = coachMarkSequencer::skip,
                )
            }
        }
    }
}

private fun propertyTypeDescription(type: PropertyType): String = when (type) {
    PropertyType.FLAT -> "Flat — a unit inside a building. Add multiple flats under one building name."
    PropertyType.VILLA -> "Villa — a standalone villa, one property, one tenant."
    PropertyType.INDEPENDENT_HOUSE -> "House — a standalone house, one property, one tenant."
    PropertyType.SHOP -> "Shop — a commercial shop or retail unit."
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
            .padding(vertical = 5.dp)
            .clip(TabBarShape)
            .then(if (selected) Modifier.background(colors.gradientStart.copy(alpha = 0.16f)) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 5.dp, horizontal = 1.dp),
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

package com.maskan.mobileapp.ui.tenant

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maskan.mobileapp.di.LocalAppContainer

/**
 * No tab bar (09-tenant-app.md): unlike the 5-tab Landlord shell, the tenant
 * shell is a single Home screen with everything else reached by pushing onto
 * one nav stack, plus "sheets" (modeled here as pushed screens too, matching
 * how this codebase already treats iOS sheets elsewhere — see
 * LandlordShellScreen's Add Property/Assign Tenant/Add Bill routes).
 */
object TenantRoutes {
    const val HOME = "tenant_home"
    const val HISTORY = "tenant_history"
    const val REQUESTS = "tenant_requests"
    const val ADD_REQUEST = "tenant_add_request"
    const val SETTINGS = "tenant_settings"
    const val CHANGE_PASSWORD = "tenant_change_password"

    private const val BILL_DETAIL_BASE = "tenant_bill_detail"
    private const val SUBMIT_PAYMENT_BASE = "tenant_submit_payment"
    const val BILL_DETAIL = "$BILL_DETAIL_BASE/{billId}"
    const val SUBMIT_PAYMENT = "$SUBMIT_PAYMENT_BASE/{billId}"

    fun billDetail(billId: String) = "$BILL_DETAIL_BASE/$billId"
    fun submitPayment(billId: String) = "$SUBMIT_PAYMENT_BASE/$billId"
}

@Composable
fun TenantShellScreen(onSignedOut: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: TenantSessionViewModel = viewModel(factory = TenantSessionViewModel.Factory(container))
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = TenantRoutes.HOME) {
        composable(TenantRoutes.HOME) {
            TenantHomeScreen(
                viewModel = viewModel,
                onAddRequest = { navController.navigate(TenantRoutes.ADD_REQUEST) },
                onOpenSettings = { navController.navigate(TenantRoutes.SETTINGS) },
                onOpenHistory = { navController.navigate(TenantRoutes.HISTORY) },
                onOpenRequests = { navController.navigate(TenantRoutes.REQUESTS) },
                onBillClick = { navController.navigate(TenantRoutes.billDetail(it)) },
            )
        }
        composable(
            TenantRoutes.BILL_DETAIL,
            arguments = listOf(navArgument("billId") { type = NavType.StringType }),
        ) { entry ->
            BillDetailScreen(
                viewModel = viewModel,
                billId = entry.arguments?.getString("billId").orEmpty(),
                onBack = { navController.popBackStack() },
                onPaySubmit = { navController.navigate(TenantRoutes.submitPayment(it)) },
            )
        }
        composable(
            TenantRoutes.SUBMIT_PAYMENT,
            arguments = listOf(navArgument("billId") { type = NavType.StringType }),
        ) { entry ->
            SubmitPaymentVerificationScreen(
                viewModel = viewModel,
                billId = entry.arguments?.getString("billId").orEmpty(),
                onDone = { navController.popBackStack(TenantRoutes.HOME, inclusive = false) },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(TenantRoutes.HISTORY) {
            TenantHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onBillClick = { navController.navigate(TenantRoutes.billDetail(it)) },
            )
        }
        composable(TenantRoutes.REQUESTS) {
            TenantRequestsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddRequest = { navController.navigate(TenantRoutes.ADD_REQUEST) },
            )
        }
        composable(TenantRoutes.ADD_REQUEST) {
            AddRequestScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(TenantRoutes.SETTINGS) {
            TenantSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(TenantRoutes.CHANGE_PASSWORD) },
                onSignedOut = onSignedOut,
            )
        }
        composable(TenantRoutes.CHANGE_PASSWORD) {
            ChangePasswordScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

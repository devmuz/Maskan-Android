package com.maskan.mobileapp.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maskan.mobileapp.data.prefs.UserRole
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.auth.LandlordLoginScreen
import com.maskan.mobileapp.ui.auth.LandlordSignUpScreen
import com.maskan.mobileapp.ui.auth.RoleSelectScreen
import com.maskan.mobileapp.ui.auth.TenantLoginScreen
import com.maskan.mobileapp.ui.landlord.LandlordShellScreen
import com.maskan.mobileapp.ui.tenant.TenantShellScreen
import kotlinx.coroutines.flow.first

object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECT = "role_select"
    const val LANDLORD_LOGIN = "landlord_login"
    const val LANDLORD_SIGNUP = "landlord_signup"
    const val TENANT_LOGIN = "tenant_login"
    const val LANDLORD_SHELL = "landlord_shell"
    const val TENANT_SHELL = "tenant_shell"
}

@Composable
fun MaskanNavHost(navController: NavHostController = rememberNavController()) {
    val container = LocalAppContainer.current

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                val user = container.authRepository.currentUser
                val role = container.rolePreferences.roleFlow.first()
                val destination = when {
                    user != null && role == UserRole.LANDLORD -> Routes.LANDLORD_SHELL
                    user != null && role == UserRole.TENANT -> Routes.TENANT_SHELL
                    else -> Routes.ROLE_SELECT
                }
                navController.navigate(destination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable(Routes.ROLE_SELECT) { RoleSelectScreen(navController) }
        composable(Routes.LANDLORD_LOGIN) { LandlordLoginScreen(navController) }
        composable(Routes.LANDLORD_SIGNUP) { LandlordSignUpScreen(navController) }
        composable(Routes.TENANT_LOGIN) { TenantLoginScreen(navController) }

        composable(Routes.LANDLORD_SHELL) {
            LandlordShellScreen(
                onSignedOut = {
                    navController.navigate(Routes.ROLE_SELECT) { popUpTo(0) { inclusive = true } }
                },
            )
        }
        composable(Routes.TENANT_SHELL) {
            TenantShellScreen(
                onSignedOut = {
                    navController.navigate(Routes.ROLE_SELECT) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}

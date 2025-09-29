package com.ahmetkaragunlu.financeai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ahmetkaragunlu.financeai.screens.auth.PasswordResetRequestScreen
import com.ahmetkaragunlu.financeai.screens.auth.PasswordResetScreen
import com.ahmetkaragunlu.financeai.screens.auth.SignInScreen
import com.ahmetkaragunlu.financeai.screens.auth.SignUpScreen
import com.ahmetkaragunlu.financeai.screens.dashboard.DashboardScreen
import com.ahmetkaragunlu.financeai.screens.splash.SplashScreen

@Composable
fun FinanceNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screens.SplashScreen.route) {
        composable(route = Screens.SplashScreen.route) {
            SplashScreen(navController = navController)
        }
        composable(route = Screens.SignInScreen.route) {
            SignInScreen(navController = navController)
        }

        composable(route= Screens.SignUpScreen.route) {
            SignUpScreen(navController = navController)
        }
        composable(route = Screens.DashboardScreen.route) {
            DashboardScreen()
        }
        composable(route = Screens.PasswordResetRequestScreen.route) {
            PasswordResetRequestScreen(navController=navController)
        }
        composable(
            route = Screens.PasswordResetScreen.route,
            arguments = listOf(
                navArgument("oobCode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "financeai://resetPassword?oobCode={oobCode}" }
            )
        ) { backStackEntry ->
            val oobCode = backStackEntry.arguments?.getString("oobCode")
            PasswordResetScreen(navController = navController, oobCode = oobCode)
        }

    }

}
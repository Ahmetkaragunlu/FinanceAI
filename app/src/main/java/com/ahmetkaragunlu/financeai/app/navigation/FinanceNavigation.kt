package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ahmetkaragunlu.financeai.app.presentation.splash.SplashScreen

@Composable
fun FinanceNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screens.SplashScreen.route
    ) {
        composable(Screens.SplashScreen.route) {
            SplashScreen(navController = navController)
        }
        authNavGraph(navController = navController)
        composable(route = Screens.MAIN_GRAPH.route) {
            MainNavGraphScaffold(navController = navController)
        }
    }
}

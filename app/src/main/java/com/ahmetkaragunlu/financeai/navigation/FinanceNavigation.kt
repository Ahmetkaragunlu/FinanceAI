package com.ahmetkaragunlu.financeai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ahmetkaragunlu.financeai.screens.auth.LoginScreen
import com.ahmetkaragunlu.financeai.screens.auth.SignUpScreen
import com.ahmetkaragunlu.financeai.screens.splash.SplashScreen

@Composable
fun FinanceNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screens.SignUpScreen.route) {
        composable(route = Screens.SplashScreen.route) {
            SplashScreen(navController = navController)
        }
        composable(route = Screens.LoginScreen.route) {
            LoginScreen(navController = navController)
        }
        composable(route= Screens.SignUpScreen.route) {
            SignUpScreen(navController = navController)
        }
        composable(route = Screens.DashboardScreen.route) {

        }
    }

}
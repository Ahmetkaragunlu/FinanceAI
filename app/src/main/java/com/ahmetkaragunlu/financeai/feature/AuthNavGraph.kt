package com.ahmetkaragunlu.financeai.feature

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.screens.auth.PasswordResetRequestScreen
import com.ahmetkaragunlu.financeai.screens.auth.PasswordResetScreen
import com.ahmetkaragunlu.financeai.screens.auth.SignInScreen
import com.ahmetkaragunlu.financeai.screens.auth.SignUpScreen

fun NavGraphBuilder.authNavGraph(navController: NavController) {

    composable(Screens.SignInScreen.route) {
        SignInScreen(navController = navController)
    }
    composable(Screens.SignUpScreen.route) {
        SignUpScreen(navController = navController)
    }
    composable(Screens.PasswordResetRequestScreen.route) {
        PasswordResetRequestScreen(navController = navController)
    }
    composable(Screens.PasswordResetScreen.route) { backStackEntry ->
        val oobCode = backStackEntry.arguments?.getString("oobCode")
        PasswordResetScreen(navController = navController, oobCode = oobCode)
    }
}
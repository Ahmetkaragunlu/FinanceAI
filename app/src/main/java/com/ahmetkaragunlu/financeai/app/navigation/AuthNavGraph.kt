package com.ahmetkaragunlu.financeai.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset.PasswordResetRequestScreen
import com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset.PasswordResetScreen
import com.ahmetkaragunlu.financeai.feature.auth.presentation.signin.SignInScreen
import com.ahmetkaragunlu.financeai.feature.auth.presentation.signup.SignUpScreen

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

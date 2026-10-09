package com.ahmetkaragunlu.financeai.app.navigation.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ahmetkaragunlu.financeai.app.navigation.MainDestination
import com.ahmetkaragunlu.financeai.app.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.app.navigation.switchRoot
import com.ahmetkaragunlu.financeai.feature.auth.destination.PasswordResetDestination
import com.ahmetkaragunlu.financeai.feature.auth.destination.PasswordResetRequestDestination
import com.ahmetkaragunlu.financeai.feature.auth.destination.SignInDestination
import com.ahmetkaragunlu.financeai.feature.auth.destination.SignUpDestination
import com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset.PasswordResetRequestRoute
import com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset.PasswordResetRoute
import com.ahmetkaragunlu.financeai.feature.auth.presentation.signin.SignInRoute
import com.ahmetkaragunlu.financeai.feature.auth.presentation.signup.SignUpRoute

fun NavGraphBuilder.authNavGraph(navController: NavController) {

    composable<SignInDestination> {
        SignInRoute(
            onSignedIn = { navController.switchRoot(MainDestination) },
            onForgotPassword = {
                navController.navigateSingleTopClear(PasswordResetRequestDestination)
            },
            onSignUp = { navController.navigateSingleTopClear(SignUpDestination) },
        )
    }
    composable<SignUpDestination> {
        SignUpRoute(onSignIn = { navController.navigateSingleTopClear(SignInDestination) })
    }
    composable<PasswordResetRequestDestination> {
        PasswordResetRequestRoute(
            onSignIn = { navController.navigateSingleTopClear(SignInDestination) }
        )
    }
    composable<PasswordResetDestination> { backStackEntry ->
        PasswordResetRoute(
            onSignIn = { navController.navigateSingleTopClear(SignInDestination) },
            oobCode = backStackEntry.toRoute<PasswordResetDestination>().oobCode,
        )
    }
}

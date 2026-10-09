package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withStateAtLeast
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ahmetkaragunlu.financeai.app.navigation.auth.authNavGraph
import com.ahmetkaragunlu.financeai.app.navigation.deeplink.FinanceDeepLink
import com.ahmetkaragunlu.financeai.app.navigation.deeplink.PendingDeepLink
import com.ahmetkaragunlu.financeai.app.navigation.main.MainNavigation
import com.ahmetkaragunlu.financeai.app.presentation.AccountViewModel
import com.ahmetkaragunlu.financeai.app.presentation.splash.SplashRoute
import com.ahmetkaragunlu.financeai.app.presentation.sync.SyncConflictDialog
import com.ahmetkaragunlu.financeai.core.money.UNSPECIFIED_CURRENCY
import com.ahmetkaragunlu.financeai.core.ui.component.LocalAccountCurrency
import com.ahmetkaragunlu.financeai.feature.auth.navigation.PasswordResetDestination
import com.ahmetkaragunlu.financeai.feature.auth.navigation.SignInDestination
import com.ahmetkaragunlu.financeai.feature.auth.presentation.session.SessionViewModel

@Composable
fun FinanceNavigation(
    deepLink: PendingDeepLink? = null,
    onDeepLinkConsumed: (String) -> Unit = {},
) {
    val accountViewModel: AccountViewModel = hiltViewModel()
    val account by accountViewModel.account.collectAsStateWithLifecycle()
    val navController = rememberFinanceNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val destination = deepLink?.destination
    LaunchedEffect(deepLink, currentEntry?.destination, account) {
        if (
            destination is FinanceDeepLink.PasswordReset &&
                currentEntry != null &&
                currentEntry?.destination?.hasRoute<SplashDestination>() != true
        ) {
            navController.navigateSingleTopClear(PasswordResetDestination(destination.code))
            onDeepLinkConsumed(deepLink.id)
        } else if (
            destination is FinanceDeepLink.Schedule &&
                account != null &&
                destination.ownerId != null &&
                destination.ownerId != account?.ownerId &&
                currentEntry?.destination?.hasRoute<MainDestination>() == true
        ) {
            // A notification for a previous account must not open that account's workflow.
            onDeepLinkConsumed(deepLink.id)
        }
    }
    CompositionLocalProvider(LocalAccountCurrency provides (account?.currencyCode ?: UNSPECIFIED_CURRENCY)) {
        NavHost(navController = navController, startDestination = SplashDestination) {
            composable<SplashDestination> {
                SplashRoute(
                    onReady = { ready ->
                        if (ready) navController.switchRoot(MainDestination)
                        else navController.switchRoot(SignInDestination)
                    }
                )
            }
            authNavGraph(navController = navController)
            composable<MainDestination> {
                val sessionViewModel: SessionViewModel = hiltViewModel()
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                LaunchedEffect(sessionViewModel.signOutComplete, lifecycle) {
                    if (sessionViewModel.signOutComplete) {
                        lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                            sessionViewModel.consumeSignOutResult()
                            navController.switchRoot(SignInDestination)
                        }
                    }
                }
                if (account != null) {
                    val userName by accountViewModel.userName.collectAsStateWithLifecycle()
                    key(account?.ownerId) {
                        MainNavigation(
                            onSignOut = sessionViewModel::performSignOut,
                            userName = userName,
                            scheduleRequestId =
                                deepLink?.id.takeIf {
                                    destination is FinanceDeepLink.Schedule &&
                                        (destination.ownerId == null ||
                                            destination.ownerId == account?.ownerId)
                                },
                            onScheduleOpened = onDeepLinkConsumed,
                        )
                        SyncConflictDialog()
                    }
                }
            }
        }
    }
}

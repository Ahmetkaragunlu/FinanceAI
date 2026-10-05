package com.ahmetkaragunlu.financeai.app.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ahmetkaragunlu.financeai.app.presentation.AccountViewModel
import com.ahmetkaragunlu.financeai.app.presentation.splash.SplashScreen
import com.ahmetkaragunlu.financeai.app.presentation.sync.SyncConflictDialog
import com.ahmetkaragunlu.financeai.core.ui.component.LocalAccountCurrency

@Composable
fun FinanceNavigation() {
    val accountViewModel: AccountViewModel = hiltViewModel()
    val account by accountViewModel.account.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    CompositionLocalProvider(LocalAccountCurrency provides (account?.currencyCode ?: "XXX")) {
    NavHost(
        navController = navController,
        startDestination = Screens.SplashScreen.route
    ) {
        composable(Screens.SplashScreen.route) {
            SplashScreen(navController = navController)
        }
        authNavGraph(navController = navController)
        composable(route = Screens.MAIN_GRAPH.route) {
            if (account != null) {
                MainNavGraphScaffold(navController = navController)
                SyncConflictDialog()
            }
        }
    }
    }
}

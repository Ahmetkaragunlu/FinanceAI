package com.ahmetkaragunlu.financeai.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditAlertDialog
import com.ahmetkaragunlu.financeai.components.EditTopBar
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.bottomnavigation.BottomBar
import com.ahmetkaragunlu.financeai.screens.auth.AuthViewModel
import com.ahmetkaragunlu.financeai.screens.main.addtransaction.AddTransactionScreen
import com.ahmetkaragunlu.financeai.screens.main.aichat.AiChatScreen
import com.ahmetkaragunlu.financeai.screens.main.budget.BudgetScreen
import com.ahmetkaragunlu.financeai.screens.main.history.DetailScreen
import com.ahmetkaragunlu.financeai.screens.main.history.TransactionHistoryScreen
import com.ahmetkaragunlu.financeai.screens.main.home.HomeScreen
import com.ahmetkaragunlu.financeai.screens.main.home.HomeViewModel
import com.ahmetkaragunlu.financeai.screens.main.schedule.ScheduledTransactionScreen


fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(Screens.HomeScreen.route) {
        HomeScreen(navController = navController)
    }

    composable(Screens.TRANSACTION_HISTORY_SCREEN.route) {
        TransactionHistoryScreen(navController = navController)
    }
    composable(
        route = MainNavConstants.DETAIL_ROUTE,
        arguments = listOf(
            navArgument(MainNavConstants.TRANSACTION_ID_ARG) {
                type = NavType.IntType
            }
        )
    ) { backStackEntry ->
        DetailScreen(navController = navController)
    }

    composable(Screens.AiChatScreen.route) {
        AiChatScreen()
    }

    composable(Screens.AnalysisScreen.route) {
        BudgetScreen()
    }

    composable(Screens.AddTransaction.route) {
        AddTransactionScreen(navController = navController)
    }

    composable(
        route = Screens.ScheduledTransactionScreen.route,
        deepLinks = listOf(
            navDeepLink { uriPattern = MainNavConstants.DEEP_LINK_PATTERN }
        )
    ) {
        ScheduledTransactionScreen()
    }
}



@Composable
fun MainNavGraphScaffold(navController: NavHostController) {

    val mainNavController: NavHostController = rememberNavController()
    val currentBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screens.HomeScreen.route

    val authViewModel: AuthViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val userName by homeViewModel.userName.collectAsStateWithLifecycle()


    HandleDeepLinks(mainNavController)

    if (homeViewModel.showLogoutDialog) {
        EditAlertDialog(
            title = R.string.sign_out_title,
            text = R.string.sign_out_message,
            onDismissRequest = { homeViewModel.showLogoutDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.showLogoutDialog = false
                    authViewModel.performSignOut {
                        navController.navigate(Screens.SignInScreen.route)
                    }
                }) {
                    Text(stringResource(R.string.yes), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { homeViewModel.showLogoutDialog = false }) {
                    Text(stringResource(R.string.no), color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            EditTopBar(
                currentRoute = currentRoute,
                navController = mainNavController,
                userName = userName,
                onLogoutClicked = { homeViewModel.showLogoutDialog = true }
            )
        },
        bottomBar = {
            BottomBar(currentRoute = currentRoute, navController = mainNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = Screens.HomeScreen.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            mainNavGraph(navController = mainNavController)
        }
    }
}




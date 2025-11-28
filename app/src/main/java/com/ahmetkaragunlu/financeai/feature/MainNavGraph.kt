package com.ahmetkaragunlu.financeai.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.components.EditTopBar
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.bottomnavigation.BottomBar
import com.ahmetkaragunlu.financeai.screens.main.AiChatScreen
import com.ahmetkaragunlu.financeai.screens.main.AnalysisScreen
import com.ahmetkaragunlu.financeai.screens.main.addtransaction.AddTransactionScreen
import com.ahmetkaragunlu.financeai.screens.main.history.DetailScreen
import com.ahmetkaragunlu.financeai.screens.main.history.TransactionHistoryScreen
import com.ahmetkaragunlu.financeai.screens.main.home.HomeScreen
import com.ahmetkaragunlu.financeai.screens.main.schedule.ScheduledTransactionScreen

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(Screens.HomeScreen.route) {
        HomeScreen()
    }
    composable(Screens.TRANSACTION_HISTORY_SCREEN.route) {
        TransactionHistoryScreen(navController = navController)
    }
    composable(
        route = "Detail_Screen/{transactionId}",
        arguments = listOf(navArgument("transactionId") {
            type = NavType.IntType
        })
    ) { backStackEntry ->
        DetailScreen(navController = navController)
    }
    composable(Screens.AiChatScreen.route) {
        AiChatScreen()
    }
    composable(Screens.AnalysisScreen.route) {
        AnalysisScreen()
    }
    composable(route = Screens.AddTransaction.route) {
        AddTransactionScreen(navController = navController)
    }
    composable(
        route = Screens.ScheduledTransactionScreen.route,
        deepLinks = listOf(
            navDeepLink { uriPattern = "financeai://main/schedule" }
        )
    ) {
        ScheduledTransactionScreen()
    }
}

@Composable
fun MainNavGraphScaffold() {
    val mainNavController: NavHostController = rememberNavController()
    val currentBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screens.HomeScreen.route
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? MainActivity
                val data = activity?.intent?.data

                if (data != null && data.scheme == "financeai" && data.host == "main") {
                    when (data.path) {
                        "/schedule" -> {
                            mainNavController.navigate(Screens.ScheduledTransactionScreen.route) {
                                popUpTo(Screens.HomeScreen.route) { inclusive = false }
                                launchSingleTop = true
                            }
                            activity.intent.data = null
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            EditTopBar(
                currentRoute = currentRoute,
                navController = mainNavController
            )
        },
        bottomBar = { BottomBar(currentRoute = currentRoute, navController = mainNavController) }
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
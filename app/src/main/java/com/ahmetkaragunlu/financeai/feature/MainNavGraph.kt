package com.ahmetkaragunlu.financeai.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahmetkaragunlu.financeai.components.EditTopBar
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.bottomnavigation.BottomBar
import com.ahmetkaragunlu.financeai.screens.main.AiChatScreen
import com.ahmetkaragunlu.financeai.screens.main.AnalysisScreen
import com.ahmetkaragunlu.financeai.screens.main.HistoryScreen
import com.ahmetkaragunlu.financeai.screens.main.HomeScreen
import com.ahmetkaragunlu.financeai.screens.transaction.AddTransactionScreen

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(Screens.HomeScreen.route) {
        HomeScreen()
    }
     composable(Screens.HISTORY_SCREEN.route) {
         HistoryScreen()
     }
    composable(Screens.AiChatScreen.route) {
        AiChatScreen()
    }
    composable(Screens.AnalysisScreen.route) {
        AnalysisScreen()
    }
    composable(route = Screens.AddTransaction.route) {
        AddTransactionScreen()
    }


}


@Composable
fun MainNavGraphScaffold() {
    val mainNavController : NavHostController = rememberNavController()
    val currentBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screens.HomeScreen.route

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

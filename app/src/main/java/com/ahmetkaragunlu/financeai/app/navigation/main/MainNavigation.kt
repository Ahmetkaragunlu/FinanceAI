package com.ahmetkaragunlu.financeai.app.navigation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.app.navigation.components.BottomBar
import com.ahmetkaragunlu.financeai.app.navigation.components.EditTopBar
import com.ahmetkaragunlu.financeai.app.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.app.navigation.rememberFinanceNavController
import com.ahmetkaragunlu.financeai.core.ui.component.EditAlertDialog
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.aichat.destination.AiChatDestination
import com.ahmetkaragunlu.financeai.feature.budget.destination.BudgetDestination
import com.ahmetkaragunlu.financeai.feature.home.destination.HomeDestination
import com.ahmetkaragunlu.financeai.feature.schedule.destination.ScheduledTransactionsDestination
import com.ahmetkaragunlu.financeai.feature.transaction.destination.AddTransactionDestination
import com.ahmetkaragunlu.financeai.feature.transaction.destination.TransactionHistoryDestination

@Composable
fun MainNavigation(
    onSignOut: () -> Unit,
    userName: String,
    scheduleRequestId: String? = null,
    onScheduleOpened: (String) -> Unit = {},
) {
    val mainNavController = rememberFinanceNavController()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    val currentEntry by mainNavController.currentBackStackEntryAsState()
    val screen = currentEntry?.destination.mainScreen()

    LaunchedEffect(scheduleRequestId) {
        if (scheduleRequestId != null) {
            mainNavController.navigate(ScheduledTransactionsDestination) {
                popUpTo<HomeDestination> { inclusive = false }
                launchSingleTop = true
            }
            onScheduleOpened(scheduleRequestId)
        }
    }

    if (showLogoutDialog) {
        EditAlertDialog(
            title = R.string.sign_out_title,
            text = R.string.sign_out_message,
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onSignOut()
                    }
                ) {
                    Text(stringResource(R.string.yes), color = FinanceColors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.no), color = FinanceColors.mutedText)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            val title = screen.localizedTitle(userName)
            EditTopBar(
                title = title,
                showBack = screen != MainScreen.HOME,
                showLogout = screen == MainScreen.HOME,
                onBackClick = {
                    if (screen == MainScreen.DETAIL) {
                        mainNavController.navigateSingleTopClear(TransactionHistoryDestination)
                    } else mainNavController.navigateSingleTopClear(HomeDestination)
                },
                onLogoutClicked = { showLogoutDialog = true },
            )
        },
        bottomBar = {
            BottomBar(
                currentScreen = screen,
                onScreenSelected = { target ->
                    when (target) {
                        MainScreen.HOME -> mainNavController.navigateSingleTopClear(HomeDestination)
                        MainScreen.HISTORY ->
                            mainNavController.navigateSingleTopClear(TransactionHistoryDestination)
                        MainScreen.ADD ->
                            mainNavController.navigateSingleTopClear(AddTransactionDestination)
                        MainScreen.AI -> mainNavController.navigateSingleTopClear(AiChatDestination)
                        MainScreen.BUDGET ->
                            mainNavController.navigateSingleTopClear(BudgetDestination)
                        else -> Unit
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            mainNavController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(padding),
        ) {
            mainNavGraph(mainNavController)
        }
    }
}

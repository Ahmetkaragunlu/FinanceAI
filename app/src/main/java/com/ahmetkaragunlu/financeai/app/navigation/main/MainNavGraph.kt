package com.ahmetkaragunlu.financeai.app.navigation.main

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ahmetkaragunlu.financeai.app.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.feature.aichat.navigation.AiChatDestination
import com.ahmetkaragunlu.financeai.feature.aichat.presentation.AiChatRoute
import com.ahmetkaragunlu.financeai.feature.budget.navigation.BudgetDestination
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetRoute
import com.ahmetkaragunlu.financeai.feature.home.navigation.HomeDestination
import com.ahmetkaragunlu.financeai.feature.home.presentation.HomeRoute
import com.ahmetkaragunlu.financeai.feature.schedule.navigation.ScheduledTransactionsDestination
import com.ahmetkaragunlu.financeai.feature.schedule.presentation.ScheduledTransactionRoute
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.AddTransactionDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionDetailDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionHistoryDestination
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.AddTransactionRoute
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.TransactionDetailRoute
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.TransactionHistoryRoute

private const val AI_SUGGESTION_PROMPT = "ai_suggestion_prompt"

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable<HomeDestination> {
        HomeRoute(
            onAiSuggestionClick = { prompt ->
                navController.navigate(AiChatDestination) { launchSingleTop = true }
                if (prompt.isNotBlank()) {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(AI_SUGGESTION_PROMPT, prompt)
                }
            }
        )
    }
    composable<TransactionHistoryDestination> {
        TransactionHistoryRoute(
            onTransactionClick = { id -> navController.navigate(TransactionDetailDestination(id)) }
        )
    }
    composable<TransactionDetailDestination> {
        TransactionDetailRoute(
            onDeleted = { navController.navigateSingleTopClear(TransactionHistoryDestination) }
        )
    }
    composable<AiChatDestination> { entry ->
        val pendingPrompt by
            entry.savedStateHandle
                .getStateFlow<String?>(AI_SUGGESTION_PROMPT, null)
                .collectAsStateWithLifecycle()
        AiChatRoute(
            initialPrompt = pendingPrompt,
            onPromptConsumed = { entry.savedStateHandle[AI_SUGGESTION_PROMPT] = null },
        )
    }
    composable<BudgetDestination> { BudgetRoute() }
    composable<AddTransactionDestination> {
        AddTransactionRoute(onSaved = { navController.navigateSingleTopClear(HomeDestination) })
    }
    // External intents pass through account-aware FinanceDeepLink validation before navigation.
    composable<ScheduledTransactionsDestination> { ScheduledTransactionRoute() }
}

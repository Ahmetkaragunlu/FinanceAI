package com.ahmetkaragunlu.financeai.app.navigation.main

import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.aichat.navigation.AiChatDestination
import com.ahmetkaragunlu.financeai.feature.budget.navigation.BudgetDestination
import com.ahmetkaragunlu.financeai.feature.schedule.navigation.ScheduledTransactionsDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.AddTransactionDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionDetailDestination
import com.ahmetkaragunlu.financeai.feature.transaction.navigation.TransactionHistoryDestination

/** Shell presentation metadata; route arguments remain owned by each feature's destination. */
enum class MainScreen(@StringRes val title: Int) {
    HOME(R.string.welcome),
    HISTORY(R.string.history_and_scheduled),
    ADD(R.string.add),
    AI(R.string.ai_assistant),
    BUDGET(R.string.budget),
    DETAIL(R.string.detail_screen),
    SCHEDULE(R.string.scheduled_screen),
}

fun NavDestination?.mainScreen(): MainScreen =
    when {
        this?.hasRoute<TransactionHistoryDestination>() == true -> MainScreen.HISTORY
        this?.hasRoute<AddTransactionDestination>() == true -> MainScreen.ADD
        this?.hasRoute<AiChatDestination>() == true -> MainScreen.AI
        this?.hasRoute<BudgetDestination>() == true -> MainScreen.BUDGET
        this?.hasRoute<TransactionDetailDestination>() == true -> MainScreen.DETAIL
        this?.hasRoute<ScheduledTransactionsDestination>() == true -> MainScreen.SCHEDULE
        else -> MainScreen.HOME
    }

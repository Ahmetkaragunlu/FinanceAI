package com.ahmetkaragunlu.financeai.feature.home.presentation

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.test.espresso.Espresso.pressBackUnconditionally
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceAITheme
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class HomeRouteTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun systemBackKeepsTheHomeRouteWithoutOpeningAnotherWorkflow() {
        val transactions = Mockito.mock(TransactionRepository::class.java)
        val budgets = Mockito.mock(BudgetRepository::class.java)
        val clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneId.systemDefault())
        val month = FinancePeriods.month(clock)
        Mockito.`when`(transactions.observeFinancialSummary(month.start, month.endExclusive))
            .thenReturn(flowOf(FinancialSummary()))
        Mockito.`when`(
            transactions.observeCategoryExpensesByTypeAndDateRange(
                TransactionType.EXPENSE, month.start, month.endExclusive,
            )
        ).thenReturn(flowOf(emptyList()))
        Mockito.`when`(budgets.observeBudgets()).thenReturn(flowOf(emptyList()))
        val viewModel = HomeViewModel(
            FinanceCalendar(clock),
            AccountSession().apply { activate("home-test", "USD", "UTC") },
            transactions,
            budgets,
        )
        val store = ViewModelStore().apply { put("home", viewModel) }
        try {
            composeRule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
            composeRule.setContent {
                FinanceAITheme {
                    HomeRoute(
                        viewModel = viewModel,
                        onAiSuggestionClick = { error("Unexpected AI navigation") },
                    )
                }
            }
            val title = composeRule.activity.getString(R.string.this_months_summary)
            composeRule.onNodeWithText(title).assertIsDisplayed()

            pressBackUnconditionally()

            composeRule.onNodeWithText(title).assertIsDisplayed()
            composeRule.runOnIdle { assertFalse(composeRule.activity.isFinishing) }
        } finally {
            composeRule.runOnIdle { store.clear() }
        }
    }
}

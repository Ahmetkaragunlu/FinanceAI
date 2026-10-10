package com.ahmetkaragunlu.financeai.feature.home.presentation

import com.ahmetkaragunlu.financeai.feature.home.presentation.suggestion.AiSuggestionState

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.core.format.formatAsCurrency
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.DateRange
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun accountCurrencyControlsFormattedAmountsWithoutChangingTheFinancialSummary() = runTest {
        val session = AccountSession().apply { activate("A", "USD") }
        val finance = Finance().apply { summary.value = FinancialSummary(200.50, 25.25) }
        val model = HomeViewModel(FinanceCalendar(Clock.systemUTC()), session, finance, Budgets())
        val store = ViewModelStore().apply { put("home", model) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.homeUiState.collect() }
        try {
            runCurrent()
            assertEquals(200.50.formatAsCurrency("USD"), model.homeUiState.value.totalIncome)
            assertEquals(25.25.formatAsCurrency("USD"), model.homeUiState.value.totalExpense)
            session.activate("B", "EUR"); runCurrent()
            assertEquals(200.50.formatAsCurrency("EUR"), model.homeUiState.value.totalIncome)
            assertEquals(175.25.formatAsCurrency("EUR"), model.homeUiState.value.remainingBalanceFormatted)
            assertEquals(175.25, model.homeUiState.value.remainingBalance, 0.0)
            session.deactivate(); runCurrent()
            assertEquals(HomeUiState(), model.homeUiState.value)
        } finally { store.clear() }
    }

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId): Clock = fixed(now, zone)
    }
    private class Finance : TransactionRepository {
        val summary = MutableStateFlow(FinancialSummary())
        val expenses = MutableStateFlow(emptyList<CategoryExpense>())
        val summaryRanges = mutableListOf<DateRange>()
        val expenseRanges = mutableListOf<DateRange>()
        override fun observeFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> {
            summaryRanges += DateRange(startDate, endDate)
            return summary
        }
        override fun observeCategoryExpensesByTypeAndDateRange(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryExpense>> {
            assertEquals(TransactionType.EXPENSE, transactionType)
            expenseRanges += DateRange(startDate, endDate)
            return expenses
        }
        override fun observeTransactionById(id: Int): Flow<Transaction?> = unused()
        override fun observeTransactions(): Flow<List<Transaction>> = unused()
        override fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = unused()
        override fun observeTransactionsByCategoryAndDate(category: CategoryType, startDate: Long, endDate: Long): Flow<List<Transaction>> = unused()
        override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> = unused()
        override fun observeTransactionsByTypeAndDate(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<Transaction>> = unused()
        override suspend fun insertTransaction(transaction: Transaction): Long = unused()
        override suspend fun updateDetails(target: Transaction, amount: Double, note: String, category: CategoryType): Unit = unused()
        override suspend fun updatePhoto(target: Transaction, photoUri: String?): String = unused()
        override suspend fun deleteTransaction(transaction: Transaction): Unit = unused()
        private fun unused(): Nothing = error("Unexpected home repository call")
    }
    private class Budgets : BudgetRepository {
        override fun observeBudgets() = flowOf(emptyList<Budget>())
        override fun observeGeneralBudget(): Flow<Budget?> = unused()
        override suspend fun getBudgetByCategory(category: CategoryType): Budget = unused()
        override suspend fun getAllBudgetsOneShot(): List<Budget> = unused()
        override suspend fun insertBudget(budget: Budget): Long = unused()
        override suspend fun deleteBudget(budget: Budget): Unit = unused()
        private fun unused(): Nothing = error("Unexpected home budget call")
    }

    @Test fun `empty month publishes a zero summary and planning rather than invented spending`() = runTest {
        val session = AccountSession().apply { activate("A", "USD") }
        val vm = HomeViewModel(FinanceCalendar(Clock.systemUTC()), session, Finance(), Budgets())
        val store = ViewModelStore().apply { put("home", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.homeUiState.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.monthlyCategoryExpenses.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.aiSuggestion.collect() }
            runCurrent()
            assertEquals(0.0, vm.homeUiState.value.remainingBalance, 0.0)
            assertEquals(0.0, vm.homeUiState.value.remainingIncomeRatio, 0.0)
            assertTrue(checkNotNull(vm.monthlyCategoryExpenses.value).isEmpty())
            assertEquals(AiSuggestionState.Planning, vm.aiSuggestion.value)
        } finally { store.clear() }
    }

    @Test fun `calendar refresh switches both queries and newly emitted financial data updates the UI`() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-09T12:00:00Z"))
        val calendar = FinanceCalendar(clock)
        val finance = Finance()
        val vm = HomeViewModel(calendar, AccountSession().apply { activate("A", "USD") }, finance, Budgets())
        val store = ViewModelStore().apply { put("home", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.homeUiState.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.monthlyCategoryExpenses.collect() }
            runCurrent()
            val october = FinancePeriods.month(clock)
            assertEquals(listOf(october), finance.summaryRanges)
            assertEquals(listOf(october), finance.expenseRanges)
            clock.now = Instant.parse("2026-11-09T12:00:00Z")
            calendar.refresh()
            runCurrent()
            val november = FinancePeriods.month(clock)
            assertEquals(listOf(october, november), finance.summaryRanges)
            assertEquals(listOf(october, november), finance.expenseRanges)
            finance.summary.value = FinancialSummary(200.0, 25.0)
            runCurrent()
            assertEquals(175.0, vm.homeUiState.value.remainingBalance, 0.0)
            assertEquals(0.875, vm.homeUiState.value.remainingIncomeRatio, 0.0)
        } finally { store.clear() }
    }
}

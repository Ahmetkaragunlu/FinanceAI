package com.ahmetkaragunlu.financeai.feature.budget.presentation

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.DateRange
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val models = ViewModelStore()
    private val repository = BudgetStore()
    private val session = AccountSession()

    private fun viewModel(calendar: FinanceCalendar = FinanceCalendar(Clock.systemUTC()), queries: FinanceQueries = FinanceQueries()): BudgetViewModel = runBlocking {
        session.activate("A", "USD")
        BudgetViewModel(calendar, session, repository, queries)
            .also { models.put("budget", it) }
    }
    private fun submit(viewModel: BudgetViewModel) = instrumentation.runOnMainSync {
        viewModel.onEvent(BudgetEvent.OnCreateGeneralBudgetClick)
        viewModel.onEvent(BudgetEvent.OnAmountChange("100"))
        viewModel.onEvent(BudgetEvent.OnSaveClick)
    }
    @After fun close() { instrumentation.runOnMainSync { models.clear() } }

    @Test fun saveMapsNetworkFailureWithoutClosingOrChangingTheForm() {
        val viewModel = viewModel()
        repository.saveFailure = DataAccessException.NetworkUnavailable()
        submit(viewModel)
        assertEquals(R.string.error_network_unavailable, viewModel.formState.value.amountErrorResId)
        assertTrue(viewModel.formState.value.isVisible)
        assertEquals("100", viewModel.formState.value.amountInput)
    }

    @Test fun duplicateDetectedDuringSaveUsesTheExistingConflictDialog() {
        val viewModel = viewModel()
        repository.saveFailure = BudgetException.DuplicateRule()
        submit(viewModel)
        assertTrue(viewModel.formState.value.isConflictDialogOpen)
        assertEquals(R.string.error_conflict_general, viewModel.formState.value.conflictErrorResId)
        assertNull(viewModel.errorResId.value)
    }

    @Test fun failedDeleteExposesConsumableErrorAndKeepsTheBudget() {
        val viewModel = viewModel()
        repository.rules = listOf(Budget(id = 1, firestoreId = "rule", ownerId = "A", currencyCode = "USD",
            budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0))
        repository.deleteFailure = DataAccessException.AccessDenied()
        instrumentation.runOnMainSync {
            viewModel.onEvent(BudgetEvent.OnDeleteClick(1))
            viewModel.onEvent(BudgetEvent.OnConfirmDelete)
        }
        assertEquals(R.string.error_access_denied, viewModel.errorResId.value)
        assertTrue(viewModel.deleteDialogState.value.isVisible)
        assertEquals(1, repository.rules.size)
        instrumentation.runOnMainSync { viewModel.consumeError() }
        assertNull(viewModel.errorResId.value)
    }

    @Test fun cancellationIsNotDisplayedAsASaveFailure() {
        val viewModel = viewModel()
        repository.saveFailure = CancellationException()
        submit(viewModel)
        assertNull(viewModel.formState.value.amountErrorResId)
        assertNull(viewModel.errorResId.value)
        repository.saveFailure = null
        instrumentation.runOnMainSync { viewModel.onEvent(BudgetEvent.OnSaveClick) }
        assertFalse(viewModel.formState.value.isVisible)
    }

    private class BudgetStore : BudgetRepository {
        private val observed = MutableStateFlow(emptyList<Budget>())
        var rules: List<Budget>
            get() = observed.value
            set(value) { observed.value = value }
        var saveFailure: Exception? = null
        var deleteFailure: Exception? = null
        val saved = mutableListOf<Budget>()
        var onSave: suspend () -> Unit = {}
        override suspend fun insertBudget(budget: Budget): Long {
            saved += budget
            onSave()
            saveFailure?.let { throw it }
            rules = rules.filterNot { it.id == budget.id } + budget
            return 1
        }
        override suspend fun deleteBudget(budget: Budget) { deleteFailure?.let { throw it }; rules = rules - budget }
        override fun observeBudgets(): Flow<List<Budget>> = observed
        override fun observeGeneralBudget() = observed.map { rows -> rows.firstOrNull { it.budgetType == BudgetType.GENERAL_MONTHLY } }
        override suspend fun getBudgetByCategory(category: CategoryType) = rules.firstOrNull { it.category == category }
        override suspend fun getAllBudgetsOneShot() = rules
    }

    private class FinanceQueries : TransactionRepository {
        val summary = MutableStateFlow(FinancialSummary())
        val summaryRanges = mutableListOf<DateRange>()
        val expenseRanges = mutableListOf<DateRange>()
        override fun observeFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> {
            summaryRanges += DateRange(startDate, endDate)
            return summary
        }
        override fun observeCategoryExpensesByTypeAndDateRange(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryExpense>> {
            expenseRanges += DateRange(startDate, endDate)
            return flowOf(emptyList())
        }
        override fun observeTransactionById(id: Int): Flow<Transaction?> = error("Unexpected query")
        override fun observeTransactions(): Flow<List<Transaction>> = error("Unexpected query")
        override fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = error("Unexpected query")
        override fun observeTransactionsByCategoryAndDate(category: CategoryType, startDate: Long, endDate: Long): Flow<List<Transaction>> = error("Unexpected query")
        override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> = error("Unexpected query")
        override fun observeTransactionsByTypeAndDate(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<Transaction>> = error("Unexpected query")
        override suspend fun insertTransaction(transaction: Transaction): Long = error("Unexpected insert")
        override suspend fun deleteTransaction(transaction: Transaction): Unit = error("Unexpected delete")
        override suspend fun updateDetails(target: Transaction, amount: Double, note: String, category: CategoryType): Unit = error("Unexpected edit")
        override suspend fun updatePhoto(target: Transaction, photoUri: String?): String? = error("Unexpected photo")
    }

    @Test fun editingGeneralAndPercentageRulesPreservesTheirIdsAndFractionalValues() {
        repository.rules = listOf(
            Budget(id = 1, firestoreId = "general", ownerId = "A", currencyCode = "USD", budgetType = BudgetType.GENERAL_MONTHLY, amount = 200.0),
            Budget(id = 2, firestoreId = "food", ownerId = "A", currencyCode = "USD", budgetType = BudgetType.CATEGORY_PERCENTAGE, category = CategoryType.FOOD, limitPercentage = 10.0)
        )
        val vm = viewModel()
        instrumentation.runOnMainSync {
            vm.onEvent(BudgetEvent.OnEditGeneralClick(GeneralBudgetState(1, 200.0, 0.0, 200.0, 0f, 0.0, 0.0)))
            vm.onEvent(BudgetEvent.OnAmountChange("250,75"))
            vm.onEvent(BudgetEvent.OnSaveClick)
        }
        assertEquals(1, repository.saved.single().id)
        assertEquals("general", repository.saved.single().firestoreId)
        assertEquals(250.75, repository.saved.single().amount, 0.0)
        instrumentation.runOnMainSync {
            vm.onEvent(BudgetEvent.OnEditCategoryClick(CategoryBudgetState(2, CategoryType.FOOD,
                BudgetType.CATEGORY_PERCENTAGE, 20.0, 0.0, 10.0, 0f, false, 0)))
            vm.onEvent(BudgetEvent.OnPercentageChange("12,5"))
            vm.onEvent(BudgetEvent.OnSaveClick)
        }
        val saved = repository.saved.last()
        assertEquals(2, saved.id)
        assertEquals("food", saved.firestoreId)
        assertEquals(12.5, checkNotNull(saved.limitPercentage), 0.0)
        assertFalse(vm.formState.value.isVisible)
    }

    @Test fun pendingSaveUsesOneOriginalSnapshotAndCannotBeSubmittedAgain(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repository.onSave = { entered.complete(Unit); release.await() }
        val vm = viewModel()
        try {
            submit(vm)
            withTimeout(5_000) { entered.await() }
            instrumentation.runOnMainSync {
                vm.onEvent(BudgetEvent.OnAmountChange("999"))
                vm.onEvent(BudgetEvent.OnSaveClick)
            }
            assertEquals(1, repository.saved.size)
            assertEquals(100.0, repository.saved.single().amount, 0.0)
            release.complete(Unit)
            withTimeout(5_000) { vm.formState.first { !it.isVisible } }
        } finally { release.complete(Unit) }
    }

    @Test fun oldAccountSaveFailureCannotAddAnErrorToTheNewAccountForm() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        repository.onSave = { entered.complete(Unit); release.await(); finished.complete(Unit); throw DataAccessException.AccessDenied() }
        val vm = viewModel()
        try {
            submit(vm)
            withTimeout(5_000) { entered.await() }
            session.activate("B", "EUR")
            instrumentation.runOnMainSync { vm.onEvent(BudgetEvent.OnAmountChange("300")) }
            release.complete(Unit)
            withTimeout(5_000) { finished.await() }
            instrumentation.waitForIdleSync()
            assertEquals("300", vm.formState.value.amountInput)
            assertNull(vm.formState.value.amountErrorResId)
            assertNull(vm.errorResId.value)
        } finally { release.complete(Unit) }
    }

    @Test fun calendarRefreshMovesBothFinanceQueriesAndPublishesTheNewSummary() = runBlocking {
        var now = Instant.parse("2026-10-09T12:00:00Z")
        val clock = object : Clock() {
            override fun instant() = now
            override fun getZone(): ZoneId = ZoneId.systemDefault()
            override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
        }
        val calendar = FinanceCalendar(clock)
        val queries = FinanceQueries()
        repository.rules = listOf(Budget(id = 1, firestoreId = "general", ownerId = "A", currencyCode = "USD",
            budgetType = BudgetType.GENERAL_MONTHLY, amount = 200.0))
        val vm = viewModel(calendar, queries)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        try {
            scope.launch { vm.uiState.collect() }
            withTimeout(5_000) { vm.uiState.first { !it.isLoading } }
            val october = FinancePeriods.month(clock)
            assertEquals(listOf(october), queries.summaryRanges)
            assertEquals(listOf(october), queries.expenseRanges)
            now = Instant.parse("2026-11-09T12:00:00Z")
            instrumentation.runOnMainSync { calendar.refresh() }
            val november = FinancePeriods.month(clock)
            withTimeout(5_000) { while (queries.expenseRanges.lastOrNull() != november || queries.summaryRanges.lastOrNull() != november) delay(10) }
            queries.summary.value = FinancialSummary(200.0, 25.0)
            withTimeout(5_000) { vm.uiState.first { it.generalBudgetState?.spentAmount == 25.0 } }
            assertEquals(175.0, checkNotNull(vm.uiState.value.generalBudgetState).remainingAmount, 0.0)
        } finally { scope.cancel() }
    }
}

package com.ahmetkaragunlu.financeai.feature.budget.presentation

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class BudgetViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val models = ViewModelStore()
    private val repository = BudgetStore()
    private val session = AccountSession()

    private fun viewModel(): BudgetViewModel = runBlocking {
        session.activate("A", "USD")
        BudgetViewModel(FinanceCalendar(Clock.systemUTC()), session, repository, FinanceQueries())
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
        var rules = emptyList<Budget>()
        var saveFailure: Exception? = null
        var deleteFailure: Exception? = null
        override suspend fun insertBudget(budget: Budget): Long {
            saveFailure?.let { throw it }; rules = rules + budget; return 1
        }
        override suspend fun deleteBudget(budget: Budget) { deleteFailure?.let { throw it }; rules = rules - budget }
        override fun observeBudgets() = flowOf(rules)
        override fun observeGeneralBudget() = flowOf(rules.firstOrNull { it.budgetType == BudgetType.GENERAL_MONTHLY })
        override suspend fun getBudgetByCategory(category: CategoryType) = rules.firstOrNull { it.category == category }
        override suspend fun getAllBudgetsOneShot() = rules
        override suspend fun updateBudget(budget: Budget): Unit = error("Unexpected update")
    }

    private class FinanceQueries : TransactionRepository {
        override fun observeFinancialSummary(startDate: Long, endDate: Long) = flowOf(FinancialSummary())
        override fun observeCategoryExpensesByTypeAndDateRange(transactionType: TransactionType, startDate: Long, endDate: Long) = flowOf(emptyList<CategoryExpense>())
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
}

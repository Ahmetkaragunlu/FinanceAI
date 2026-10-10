package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.core.time.DateFilter
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()
    private val clock = Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun reselectingTheSameTypeKeepsCategoryPriorityAndItsCurrentDateRange() = runTest {
        val repository = RecordingRepository()
        val model = TransactionHistoryViewModel(repository, FinanceCalendar(clock))
        val store = ViewModelStore().apply { put("history", model) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.transactions.collect() }
        try {
            model.onTypeSelected(TransactionType.EXPENSE)
            model.onCategorySelected(CategoryType.FOOD)
            model.onDateSelected(DateFilter.TODAY)
            runCurrent()
            val original = repository.last
            model.onTypeSelected(TransactionType.EXPENSE); runCurrent()
            assertEquals(CategoryType.FOOD, model.filters.value.category)
            assertEquals(original, repository.last)
            assertNull(repository.last.type)
            model.onCategorySelected(null); runCurrent()
            assertEquals(TransactionType.EXPENSE, repository.last.type)
            assertEquals(original.start, repository.last.start)
            assertEquals(original.end, repository.last.end)
        } finally {
            store.clear()
        }
    }

    @Test
    fun changingTypeClearsCategoryAndUpdatesTheQueryFromOneFilterSnapshot() = runTest {
        val repository = RecordingRepository()
        val viewModel = TransactionHistoryViewModel(repository, FinanceCalendar(clock))
        val store = ViewModelStore().apply { put("history", viewModel) }
        val collector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.transactions.collect()
            }
        try {
            runCurrent()
            assertFalse(viewModel.canOpenCategoryMenu())
            assertTrue(viewModel.showCategoryError)
            viewModel.onTypeSelected(TransactionType.EXPENSE)
            viewModel.onCategorySelected(CategoryType.FOOD)
            runCurrent()
            assertEquals(CategoryType.FOOD, repository.last.category)
            viewModel.onTypeSelected(TransactionType.INCOME)
            runCurrent()
            assertNull(viewModel.filters.value.category)
            assertNull(repository.last.category)
            assertEquals(TransactionType.INCOME, repository.last.type)
            assertFalse(viewModel.showCategoryError)
        } finally {
            collector.cancel()
            store.clear()
        }
    }

    @Test
    fun selectingDateChangesTheActualRangeAndAllDatesRestoresTheUnboundedQuery() = runTest {
        val repository = RecordingRepository()
        val viewModel = TransactionHistoryViewModel(repository, FinanceCalendar(clock))
        val store = ViewModelStore().apply { put("history", viewModel) }
        val collector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.transactions.collect()
            }
        try {
            runCurrent()
            viewModel.onDateSelected(DateFilter.TODAY)
            runCurrent()
            val range =
                FinancePeriods.filter(DateFilter.TODAY, clock.withZone(ZoneId.systemDefault()))
            assertEquals(range.start, repository.last.start)
            assertEquals(range.endExclusive, repository.last.end)
            viewModel.onDateSelected(DateFilter.ALL)
            runCurrent()
            assertEquals(Query(), repository.last)
        } finally {
            collector.cancel()
            store.clear()
        }
    }

    private data class Query(
        val type: TransactionType? = null,
        val category: CategoryType? = null,
        val start: Long = 0,
        val end: Long = Long.MAX_VALUE,
    )

    private class RecordingRepository : TransactionRepository {
        var last = Query()

        private fun query(value: Query): Flow<List<Transaction>> {
            last = value
            return flowOf(emptyList())
        }

        override fun observeTransactions() = query(Query())

        override fun observeTransactionsByDateRange(startDate: Long, endDate: Long) =
            query(Query(start = startDate, end = endDate))

        override fun observeTransactionsByCategoryAndDate(
            category: CategoryType,
            startDate: Long,
            endDate: Long,
        ) = query(Query(category = category, start = startDate, end = endDate))

        override fun observeTransactionsByTypeAndDate(
            transactionType: TransactionType,
            startDate: Long,
            endDate: Long,
        ) = query(Query(type = transactionType, start = startDate, end = endDate))

        override fun observeTransactionById(id: Int): Flow<Transaction?> = flowOf(null)

        override fun observeFinancialSummary(startDate: Long, endDate: Long) =
            flowOf(FinancialSummary())

        override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
            flowOf(0.0)

        override fun observeCategoryExpensesByTypeAndDateRange(
            transactionType: TransactionType,
            startDate: Long,
            endDate: Long,
        ): Flow<List<CategoryExpense>> = flowOf(emptyList())

        override suspend fun insertTransaction(transaction: Transaction) = error("Not a query")

        override suspend fun updateDetails(
            target: Transaction,
            amount: Double,
            note: String,
            category: CategoryType
        ) = error("Not a query")

        override suspend fun updatePhoto(target: Transaction, photoUri: String?): String? =
            error("Not a query")

        override suspend fun deleteTransaction(transaction: Transaction) = error("Not a query")
    }
}

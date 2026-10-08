package com.ahmetkaragunlu.financeai.feature.schedule.presentation

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ScheduledTransactionsViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val models = ViewModelStore()
    private val session = AccountSession()
    private val plan = ScheduledTransaction(id = 1, firestoreId = "plan", amount = 50.0,
        ownerId = "A", currencyCode = "USD", type = TransactionType.EXPENSE,
        category = CategoryType.FOOD, note = null, scheduledDate = 100)
    private fun viewModel(action: suspend () -> Unit): ScheduledTransactionsViewModel = runBlocking {
        session.activate("A", "USD")
        val complete = object : CompleteScheduledTransaction {
            override suspend fun invoke(value: ScheduledTransaction): Transaction? { action(); return null }
        }
        ScheduledTransactionsViewModel(session, complete, Plans()).also { models.put("schedule", it) }
    }
    @After fun close() { instrumentation.runOnMainSync { models.clear() } }

    @Test fun completionFailureUsesSpecificXmlErrorAndCanBeConsumed() {
        val viewModel = viewModel { throw DataAccessException.NetworkUnavailable() }
        instrumentation.runOnMainSync { viewModel.executeScheduledTransaction(plan) }
        assertEquals(R.string.error_network_unavailable, viewModel.errorResId.value)
        instrumentation.runOnMainSync { viewModel.consumeError() }
        assertNull(viewModel.errorResId.value)
    }
    @Test fun unknownDiagnosticAndCancellationNeverReachTheUserAsRawText() {
        val viewModel = viewModel { throw IllegalStateException("private diagnostic") }
        instrumentation.runOnMainSync { viewModel.executeScheduledTransaction(plan) }
        assertEquals(R.string.error_operation_retry, viewModel.errorResId.value)
        val cancelled = viewModel { throw CancellationException() }
        instrumentation.runOnMainSync { cancelled.executeScheduledTransaction(plan) }
        assertNull(cancelled.errorResId.value)
    }
    @Test fun oldAccountCompletionFailureDoesNotNotifyTheNewAccount() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel { gate.await() }
        instrumentation.runOnMainSync { viewModel.executeScheduledTransaction(plan) }
        session.activate("B", "EUR")
        gate.completeExceptionally(DataAccessException.AccessDenied())
        instrumentation.waitForIdleSync()
        assertNull(viewModel.errorResId.value)
    }
    private class Plans : ScheduledTransactionRepository {
        override fun observeScheduledTransactions() = flowOf(emptyList<ScheduledTransaction>())
        override suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long = error("Unexpected insert")
        override suspend fun updateScheduledTransaction(transaction: ScheduledTransaction): Unit = error("Unexpected update")
        override suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction): Unit = error("Unexpected delete")
        override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction? = error("Unexpected query")
        override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction? = error("Unexpected query")
    }
}

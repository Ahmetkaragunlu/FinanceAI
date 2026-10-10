package com.ahmetkaragunlu.financeai.notification.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.testing.workerSessions
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class NotificationActionWorkerTest {
    private class Fixture {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = AccountSession().apply { activate("A", "USD") }
        val repository = mock(ScheduledTransactionRepository::class.java)
        val completion = mock(CompleteScheduledTransaction::class.java)
        val reminders = mock(ReminderCoordinator::class.java)
        val sessions = workerSessions(session)
        fun worker(input: Data): NotificationActionWorker {
            val factory = object : WorkerFactory() {
                override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                    NotificationActionWorker(context, parameters, sessions, repository, completion, reminders)
            }
            return TestListenableWorkerBuilder<NotificationActionWorker>(context).setWorkerFactory(factory).setInputData(input).build()
        }
        fun input(action: String) = workDataOf("account_owner_id" to "A", "firestore_id" to "plan", "action" to action, "requested_at" to 1234L)
    }

    @Test fun invalidInputAndUnsupportedActionsFailWithoutDomainDispatch(): Unit = runBlocking {
        val f = Fixture()
        assertEquals(ListenableWorker.Result.failure(), f.worker(Data.EMPTY).doWork())
        val missingAction = workDataOf("account_owner_id" to "A", "firestore_id" to "plan")
        assertEquals(ListenableWorker.Result.failure(), f.worker(missingAction).doWork())
        assertEquals(ListenableWorker.Result.failure(), f.worker(f.input("unknown")).doWork())
        verifyNoInteractions(f.repository, f.completion, f.reminders)
    }

    @Test fun unsupportedActionForAnotherAccountRemainsANoOpBeforeActionParsing(): Unit = runBlocking {
        val f = Fixture()
        f.session.activate("B", "EUR")
        assertEquals(ListenableWorker.Result.success(), f.worker(f.input("unknown")).doWork())
        verifyNoInteractions(f.repository, f.completion, f.reminders)
    }

    @Test fun confirmationCompletesTheExistingPlanBeforeDismissingItsNotification(): Unit = runBlocking {
        val f = Fixture()
        val plan = ScheduledTransaction(id = 7, firestoreId = "plan", ownerId = "A", currencyCode = "USD",
            amount = 10.0, type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = 100)
        `when`(f.repository.getScheduledTransactionByFirestoreId("plan")).thenReturn(plan)
        assertEquals(ListenableWorker.Result.success(), f.worker(f.input("com.ahmetkaragunlu.financeai.ACTION_CONFIRM")).doWork())
        val order = inOrder(f.completion, f.reminders)
        order.verify(f.completion).invoke(plan)
        order.verify(f.reminders).dismiss(f.session.requireAccount(), "plan")
    }

    @Test fun legacyCancelStillMeansSnoozeAndDismissDoesNotCompleteAPlan(): Unit = runBlocking {
        val f = Fixture()
        for (action in listOf("com.ahmetkaragunlu.financeai.ACTION_CANCEL", "com.ahmetkaragunlu.financeai.ACTION_SNOOZE")) {
            assertEquals(ListenableWorker.Result.success(), f.worker(f.input(action)).doWork())
        }
        verify(f.reminders, times(2)).snooze(f.session.requireAccount(), "plan", 1234L)
        assertEquals(ListenableWorker.Result.success(), f.worker(f.input("com.ahmetkaragunlu.financeai.ACTION_DISMISS")).doWork())
        verify(f.reminders).dismiss(f.session.requireAccount(), "plan")
        verifyNoInteractions(f.completion)
    }

    @Test fun domainFailureRetriesButCancellationPropagates(): Unit = runBlocking {
        val f = Fixture()
        doAnswer { throw IOException("synthetic offline") }.`when`(f.reminders).snooze(f.session.requireAccount(), "plan", 1234L)
        assertEquals(ListenableWorker.Result.retry(), f.worker(f.input("com.ahmetkaragunlu.financeai.ACTION_SNOOZE")).doWork())
        doAnswer { throw CancellationException() }.`when`(f.reminders).snooze(f.session.requireAccount(), "plan", 1234L)
        assertThrows(CancellationException::class.java) {
            runBlocking { f.worker(f.input("com.ahmetkaragunlu.financeai.ACTION_SNOOZE")).doWork() }
        }
    }
}

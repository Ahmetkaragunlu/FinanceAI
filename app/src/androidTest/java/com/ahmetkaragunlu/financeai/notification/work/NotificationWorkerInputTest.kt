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
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class NotificationWorkerInputTest {
    private class Fixture {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = AccountSession().apply { activate("A", "USD") }
        val sessions = mock(SessionCoordinator::class.java)
        val reminders = mock(ReminderCoordinator::class.java)
        val repository = mock(ScheduledTransactionRepository::class.java)

        init { `when`(sessions.session).thenReturn(session) }

        fun worker(input: Data): NotificationWorker {
            val factory = object : WorkerFactory() {
                override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                    NotificationWorker(context, parameters, sessions, reminders, repository)
            }
            return TestListenableWorkerBuilder<NotificationWorker>(context)
                .setInputData(input).setWorkerFactory(factory).build()
        }
    }

    @Test fun persistedRemoteInputStillProcessesTheSameOwnedPlan(): Unit = runBlocking {
        val f = Fixture()
        assertEquals(ListenableWorker.Result.success(), f.worker(
            workDataOf("account_owner_id" to "A", "firestore_id" to "remote-plan")).doWork())
        verify(f.reminders).process(f.session.requireAccount(), "remote-plan")
    }

    @Test fun persistedLocalIdKeepsItsFallbackAndMissingRecordStillRestoresCurrentReminders(): Unit = runBlocking {
        val f = Fixture()
        val plan = ScheduledTransaction(id = 7, firestoreId = "local-plan", ownerId = "A", currencyCode = "USD",
            amount = 25.0, type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = 100)
        `when`(f.repository.getScheduledTransactionById(7)).thenReturn(plan)
        assertEquals(ListenableWorker.Result.success(), f.worker(
            workDataOf("account_owner_id" to "A", "transaction_id" to 7L)).doWork())
        verify(f.reminders).process(f.session.requireAccount(), "local-plan")
        assertEquals(ListenableWorker.Result.success(), f.worker(workDataOf("account_owner_id" to "A")).doWork())
        verify(f.repository).getScheduledTransactionById(-1)
        verify(f.reminders).restoreCurrent()
    }
}

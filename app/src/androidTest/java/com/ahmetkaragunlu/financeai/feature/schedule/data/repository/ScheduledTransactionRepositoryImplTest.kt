package com.ahmetkaragunlu.financeai.feature.schedule.data.repository

import android.database.sqlite.SQLiteException
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.notification.presentation.ReminderPresenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduledTransactionRepositoryImplTest {
    private fun repository(f: AccountDatabaseFixture): ScheduledTransactionRepositoryImpl {
        val presenter = object : ReminderPresenter {
            override fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String) = false
            override fun cancel(ownerId: String, remoteId: String) = Unit
        }
        return ScheduledTransactionRepositoryImpl(
            f.database.scheduledTransactionDao(), f.database, f.session, f.pending, f.scheduler,
            ReminderScheduler(f.workManager, f.clock), presenter
        )
    }

    private fun draft() = ScheduledTransaction(firestoreId = "plan", amount = 25.50,
        type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = 100)

    @Test fun accountSwitchHidesPlansAndRejectsOldAccountUpdatesAndDeletes() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val repository = repository(f)
            repository.insertScheduledTransaction(draft())
            val stored = repository.observeScheduledTransactions().first().single()
            f.activate("B", "EUR")
            assertTrue(repository.observeScheduledTransactions().first().isEmpty())
            assertNull(repository.getScheduledTransactionById(stored.id))
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.insertScheduledTransaction(stored.copy(amount = 100.0)) } }
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.deleteScheduledTransaction(stored) } }
            assertEquals(1, f.database.syncRecordDao().pending("A").size)
            f.activate()
            assertEquals(stored, repository.observeScheduledTransactions().first().single())
        }
    }

    @Test fun outboxFailureRollsBackCreationEditingAndDeletion() = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val repository = repository(f)
            f.rejectSyncWrites()
            assertThrows(SQLiteException::class.java) { runBlocking { repository.insertScheduledTransaction(draft()) } }
            assertTrue(repository.observeScheduledTransactions().first().isEmpty())
            assertTrue(f.database.syncRecordDao().pending("A").isEmpty())
            f.allowSyncWrites()
            repository.insertScheduledTransaction(draft())
            val stored = repository.observeScheduledTransactions().first().single()
            f.rejectSyncWrites()
            assertThrows(SQLiteException::class.java) { runBlocking { repository.insertScheduledTransaction(stored.copy(note = "edited")) } }
            assertThrows(SQLiteException::class.java) { runBlocking { repository.deleteScheduledTransaction(stored) } }
            assertEquals(stored, repository.observeScheduledTransactions().first().single())
            f.allowSyncWrites()
            repository.deleteScheduledTransaction(stored)
            assertTrue(repository.observeScheduledTransactions().first().isEmpty())
            val pending = checkNotNull(f.database.syncRecordDao().get("A", "scheduled_transactions", stored.firestoreId))
            assertTrue(pending.pendingDelete)
            assertNotNull(pending.mutationId)
        }
    }
}

package com.ahmetkaragunlu.financeai.feature.schedule.data.remote

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ReminderState
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduleCommand
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class SharedReminderRemoteStoreTest {
    private suspend fun plan(f: AccountDatabaseFixture): Long = f.database.scheduledTransactionDao().insertScheduledTransaction(
        ScheduledTransactionEntity(ownerId = "A", firestoreId = "plan", currencyCode = "USD", amountMinor = 1000,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = 100, photoUri = "/synthetic/retained.jpg"))
    private fun data(status: String = "active", revision: Long = 4) = mapOf<String, Any?>("status" to status,
        "scheduledDate" to 100L, "revision" to revision, "snoozeAt" to 999L, "deleteAt" to 2000L)

    @Test fun normalizationRejectsInvalidStateAndKeepsOnlyTheServerStateContract(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val store = SharedReminderRemoteStore(f.database, ReminderScheduler(f.workManager, f.clock), mock(ReminderPresenter::class.java))
            assertEquals(data(), store.normalize(data() + mapOf("userId" to "A", "lastShownAt" to 10L), f.session.requireAccount()))
            for (bad in listOf(data("unknown"), data() + ("scheduledDate" to "100"), data() - "revision")) {
                assertThrows(DataAccessException.InvalidRemoteData::class.java) { store.normalize(bad, f.session.requireAccount()) }
            }
        }
    }

    @Test fun oldRevisionCannotReplaceStateAndPendingSnoozeKeepsItsDeviceDisplayReceipts(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate(); plan(f)
            val scheduler = spy(ReminderScheduler(f.workManager, f.clock))
            val presenter = mock(ReminderPresenter::class.java)
            val store = SharedReminderRemoteStore(f.database, scheduler, presenter)
            val previous = ReminderState("A", "plan", 100, automaticSlots = 1, snoozeAt = 300,
                consumedSnoozeAt = 50, lastShownAt = 80, revision = 3)
            f.database.reminderStateDao().save(previous)
            store.apply(f.session.requireAccount(), "plan", data(revision = 2))
            assertEquals(previous, f.database.reminderStateDao().get("A", "plan"))
            f.database.scheduleCommandDao().insert(ScheduleCommand("snooze", "A", "plan", 100, "snooze", 200))
            store.apply(f.session.requireAccount(), "plan", data())
            val next = checkNotNull(f.database.reminderStateDao().get("A", "plan"))
            assertEquals(300L, next.snoozeAt)
            assertEquals(80L, next.lastShownAt)
            assertEquals(1, next.automaticSlots)
            assertEquals(null, next.expiredShownAt)
            assertEquals(2000L, next.deleteAt)
            verify(scheduler).wake("A", "plan", f.clock.millis())
        }
    }

    @Test fun terminalStateKeepsDirtyPlanAndMediaButRemovesCleanOrPendingDeleteRows(): Unit = runBlocking {
        for (dirty in listOf(true, false)) AccountDatabaseFixture().use { f ->
            f.activate(); val id = plan(f)
            val scheduler = spy(ReminderScheduler(f.workManager, f.clock))
            val presenter = mock(ReminderPresenter::class.java)
            if (dirty) f.database.syncRecordDao().save(SyncRecord("A", "scheduled_transactions", "plan", mutationId = "edit"))
            val store = SharedReminderRemoteStore(f.database, scheduler, presenter)
            store.apply(f.session.requireAccount(), "plan", data("completed"))
            if (dirty) assertNotNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            else assertNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            assertFalse(checkNotNull(f.database.reminderStateDao().get("A", "plan")).active)
            verify(scheduler).cancel("A", "plan", id)
            verify(presenter).cancel("A", "plan")
            if (dirty) {
                f.database.syncRecordDao().save(SyncRecord("A", "scheduled_transactions", "plan", mutationId = "delete", pendingDelete = true))
                store.apply(f.session.requireAccount(), "plan", data("deleted", 5))
                assertNull(f.database.scheduledTransactionDao().getScheduledTransactionByFirestoreId("plan"))
            }
        }
    }

    @Test fun newerSnoozeCancelsOldPresentationAndNewDateResetsOnlyOldDeviceProgress(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate(); val id = plan(f)
            val presenter = mock(ReminderPresenter::class.java)
            val scheduler = spy(ReminderScheduler(f.workManager, f.clock))
            val store = SharedReminderRemoteStore(f.database, scheduler, presenter)
            f.database.reminderStateDao().save(ReminderState("A", "plan", 100, automaticSlots = 3, lastShownAt = 50, revision = 3))
            store.apply(f.session.requireAccount(), "plan", data())
            verify(presenter).cancel("A", "plan")
            verify(scheduler).cancel("A", "plan", id)
            store.apply(f.session.requireAccount(), "plan", data(revision = 5) + ("scheduledDate" to 200L))
            val next = checkNotNull(f.database.reminderStateDao().get("A", "plan"))
            assertEquals(0, next.automaticSlots)
            assertNull(next.lastShownAt)
            assertNull(next.expiredShownAt)
        }
    }
}

package com.ahmetkaragunlu.financeai.feature.schedule.data

import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncPayload
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.repository.ScheduledTransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.schedule.data.sync.ScheduleCommandQueue
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.transaction.data.repository.TransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.notification.presentation.ReminderPresenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RoomScheduledCompletionTest {
    private lateinit var fixture: AccountDatabaseFixture
    private lateinit var schedules: ScheduledTransactionRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var complete: RoomScheduledCompletion

    @Before fun setup() = runBlocking {
        fixture = AccountDatabaseFixture()
        fixture.activate()
        val presenter = object : ReminderPresenter {
            override fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String) = false
            override fun cancel(ownerId: String, remoteId: String) = Unit
        }
        schedules = ScheduledTransactionRepositoryImpl(
            fixture.database.scheduledTransactionDao(), fixture.database, fixture.session,
            fixture.pending, fixture.scheduler, ReminderScheduler(fixture.workManager, fixture.clock), presenter
        )
        transactions = TransactionRepositoryImpl(
            fixture.database.transactionDao(), fixture.database, fixture.session, fixture.pending, fixture.scheduler
        )
        complete = RoomScheduledCompletion(
            fixture.database, fixture.session, fixture.pending, fixture.scheduler, fixture.clock,
            ScheduleCommandQueue(fixture.database, fixture.clock)
        )
    }

    @After fun close() = fixture.close()

    @Test fun scheduledCompletionIsAtomicAndRepeatedCallDoesNotDuplicateMoney() = runBlocking {
        val id = schedules.insertScheduledTransaction(ScheduledTransaction(firestoreId = "plan", amount = 50.25,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "Plan", scheduledDate = 100))
        val plan = checkNotNull(schedules.getScheduledTransactionById(id))
        assertNotNull(complete(plan))
        assertNull(complete(plan))
        assertEquals(1, transactions.observeTransactions().first().size)
        assertTrue(schedules.observeScheduledTransactions().first().isEmpty())
        assertEquals(2, fixture.database.syncRecordDao().pending("A").size)
        assertEquals("complete", fixture.database.scheduleCommandDao().forAccount("A").single().type)
    }

    @Test fun scheduledCompletionRetainsRemotePhotoMetadataWithoutUploadingCachedFile() = runBlocking {
        val id = schedules.insertScheduledTransaction(ScheduledTransaction(firestoreId = "photo-plan", amount = 50.25,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "Photo plan", scheduledDate = 100,
            photoUri = "/account/A/SYNC_cached.jpg"))
        val record = checkNotNull(fixture.database.syncRecordDao().get("A", "scheduled_transactions", "photo-plan"))
        val metadata = mapOf("photoStorageUrl" to "https://example.test/receipt", "photoRemoved" to false, "photoVersion" to "v2")
        fixture.database.syncRecordDao().save(record.copy(pendingPayload = SyncPayload.encode(SyncPayload.decode(checkNotNull(record.pendingPayload)) + metadata)))
        val completed = checkNotNull(complete(checkNotNull(schedules.getScheduledTransactionById(id))))
        val outgoing = SyncPayload.decode(checkNotNull(fixture.database.syncRecordDao().get("A", "transactions", completed.firestoreId)?.pendingPayload))
        assertEquals("/account/A/SYNC_cached.jpg", completed.photoUri)
        metadata.forEach { (key, expected) -> assertEquals(expected, outgoing[key]) }
        assertFalse(outgoing.containsKey("photoUri"))
        assertTrue(checkNotNull(fixture.database.syncRecordDao().get("A", "scheduled_transactions", "photo-plan")).pendingDelete)
    }
}

package com.ahmetkaragunlu.financeai.feature.transaction.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.session.*
import com.ahmetkaragunlu.financeai.core.sync.*
import com.ahmetkaragunlu.financeai.core.time.FinancePeriods
import com.ahmetkaragunlu.financeai.feature.schedule.data.RoomScheduledCompletion
import com.ahmetkaragunlu.financeai.feature.schedule.data.repository.ScheduledTransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import dagger.Lazy
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryImplTest {
    private lateinit var database: FinanceDatabase
    private lateinit var session: AccountSession
    private lateinit var repository: TransactionRepositoryImpl
    private lateinit var scheduler: SyncScheduler
    private lateinit var pending: PendingChanges

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        session = AccountSession()
        activate("A", "USD")
        scheduler = SyncScheduler(WorkManager.getInstance(context))
        pending = PendingChanges(database)
        repository = TransactionRepositoryImpl(database.transactionDao(), database, session, pending, scheduler)
    }
    @After fun close() { database.close(); WorkManagerTestInitHelper.closeWorkDatabase() }

    private suspend fun activate(ownerId: String, currency: String) {
        database.accountDao().save(AccountPreferences(ownerId, currency))
        database.accountDao().setActive(ActiveAccountRow(ownerId = ownerId))
        session.activate(ownerId, currency)
    }
    private fun value(remoteId: String = "receipt") = Transaction(firestoreId = remoteId,
        amount = 125.50, date = 100L, transaction = TransactionType.EXPENSE, category = CategoryType.FOOD)

    @Test fun generatedIdentityAndPendingAreStoredTogether() = runBlocking {
        val id = repository.insertTransaction(value())
        assertTrue(id > 0)
        val stored = repository.observeTransactionById(id.toInt()).first()!!
        assertEquals(id.toInt(), stored.id)
        assertEquals(125.50, stored.amount, 0.0)
        assertEquals("A", stored.ownerId)
        assertNotNull(database.syncRecordDao().get("A", "transactions", "receipt")!!.mutationId)
    }
    @Test fun logoutPreservesPendingAndOtherAccountCannotSeeIt() = runBlocking {
        val id = repository.insertTransaction(value())
        session.deactivate(); database.accountDao().clearActive()
        assertTrue(repository.observeTransactions().first().isEmpty())
        activate("B", "EUR")
        assertTrue(repository.observeTransactions().first().isEmpty())
        assertNull(repository.observeTransactionById(id.toInt()).first())
        assertEquals(1, database.syncRecordDao().pending("A").size)
        activate("A", "USD")
        assertEquals(1, repository.observeTransactions().first().size)
    }
    @Test fun deleteRetainsTombstoneAndRemoteIdentity() = runBlocking {
        repository.insertTransaction(value())
        val stored = repository.observeTransactions().first().single()
        repository.deleteTransaction(stored)
        assertTrue(repository.observeTransactions().first().isEmpty())
        assertTrue(database.syncRecordDao().get("A", "transactions", "receipt")!!.pendingDelete)
    }
    @Test fun updateKeepsPrimaryKeyAndRejectsOldAccountMutation() = runBlocking {
        val id = repository.insertTransaction(value())
        val stored = repository.observeTransactions().first().single()
        repository.updateTransaction(stored.copy(amount = 150.25))
        assertEquals(id.toInt(), repository.observeTransactions().first().single().id)
        activate("B", "EUR")
        try { repository.updateTransaction(stored); fail("Old account mutation accepted") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun exclusiveDateBoundaryDoesNotDoubleCountNextPeriod() = runBlocking {
        repository.insertTransaction(value("first").copy(date = 100L))
        repository.insertTransaction(value("next").copy(date = 200L))
        assertEquals(1, repository.observeTransactionsByDateRange(100, 200).first().size)
        assertEquals(125.50, repository.observeTotalExpenseByDateRange(100, 200).first()!!, 0.0)
    }
    @Test fun outboxFailureRollsBackLocalFinancialMutation() = runBlocking {
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_pending BEFORE INSERT ON sync_records BEGIN SELECT RAISE(ABORT, 'injected outbox failure'); END")
        try { repository.insertTransaction(value()); fail("Mutation accepted without durable pending") }
        catch (_: SQLiteException) { }
        assertTrue(repository.observeTransactions().first().isEmpty())
        assertTrue(database.syncRecordDao().pending("A").isEmpty())
    }

    @Test fun financialSummaryUsesExactAmountsExclusivePeriodAndActiveAccount() = runBlocking {
        repository.insertTransaction(value("income").copy(amount = 0.30, transaction = TransactionType.INCOME))
        repository.insertTransaction(value("expense").copy(amount = 0.10))
        repository.insertTransaction(value("next-period").copy(amount = 99.99, date = 200))
        val summary = repository.observeFinancialSummary(100, 200).first()
        assertEquals(0.30, summary.income, 0.0)
        assertEquals(0.10, summary.expense, 0.0)
        assertEquals(0.20, summary.remainingBalance, 0.0)
        activate("B", "EUR")
        val otherAccount = repository.observeFinancialSummary(100, 200).first()
        assertEquals(0.0, otherAccount.income, 0.0)
        assertEquals(0.0, otherAccount.expense, 0.0)
    }

    @Test fun calendarMonthIncludesOctoberLastDayButExcludesSeptemberAndNovember() = runBlocking {
        val zone = ZoneId.of("Europe/Istanbul")
        fun date(value: String) = LocalDate.parse(value).atStartOfDay(zone).toInstant()
        val range = FinancePeriods.month(Clock.fixed(date("2026-10-05"), zone))
        repository.insertTransaction(value("september").copy(amount = 999.0, date = date("2026-09-30").toEpochMilli()))
        repository.insertTransaction(value("october-income").copy(amount = 100.0, transaction = TransactionType.INCOME,
            date = date("2026-10-01").toEpochMilli()))
        repository.insertTransaction(value("october-expense").copy(amount = 25.0, date = range.endExclusive - 1))
        repository.insertTransaction(value("november").copy(amount = 500.0, date = date("2026-11-01").toEpochMilli()))

        val summary = repository.observeFinancialSummary(range.start, range.endExclusive).first()
        assertEquals(100.0, summary.income, 0.0)
        assertEquals(25.0, summary.expense, 0.0)
        assertEquals(75.0, summary.remainingBalance, 0.0)
        assertEquals(listOf(CategoryExpense("FOOD", 25.0)), repository.observeCategoryExpensesByTypeAndDateRange(
            TransactionType.EXPENSE, range.start, range.endExclusive).first())
    }

    @Test fun remoteAcknowledgementPreservesPrimaryKeyAndLocalPhotoUntilUpload() = runBlocking {
        val id = repository.insertTransaction(value().copy(photoUri = "/local/retained.jpg"))
        val account = session.requireAccount()
        val photos = PhotoRemoteCache(ApplicationProvider.getApplicationContext(), Lazy { error("No media network request in apply test") }, session, Dispatchers.IO)
        val remote = TransactionRemoteStore(database, photos)
        val record = database.syncRecordDao().get("A", "transactions", "receipt")!!
        val payload = SyncPayload.decode(record.pendingPayload!!)
        assertEquals(false, payload["photoRemoved"])
        assertTrue(payload.containsKey("photoStorageUrl"))
        remote.apply(account, "receipt", payload)
        val retained = repository.observeTransactions().first().single()
        assertEquals(id.toInt(), retained.id)
        assertEquals("/local/retained.jpg", retained.photoUri)
        remote.apply(account, "receipt", payload + mapOf("photoRemoved" to true))
        assertNull(repository.observeTransactions().first().single().photoUri)
    }

    @Test fun newEditsPreserveRemotePhotoButExplicitRemovalIsDurable() = runBlocking {
        repository.insertTransaction(value().copy(photoUri = "https://example.test/photo"))
        val old = repository.observeTransactions().first().single()
        val record = database.syncRecordDao().get("A", "transactions", "receipt")!!
        database.syncRecordDao().save(record.copy(basePayload = SyncPayload.encode(SyncPayload.decode(record.pendingPayload!!) + mapOf("photoStorageUrl" to old.photoUri)),
            pendingPayload = null, mutationId = null))
        repository.updateTransaction(old.copy(note = "edited"))
        assertEquals(old.photoUri, SyncPayload.decode(database.syncRecordDao().get("A", "transactions", "receipt")!!.pendingPayload!!)["photoStorageUrl"])
        repository.updateTransaction(old.copy(photoUri = null))
        val removed = SyncPayload.decode(database.syncRecordDao().get("A", "transactions", "receipt")!!.pendingPayload!!)
        assertEquals(true, removed["photoRemoved"])
        assertNull(removed["photoStorageUrl"])
    }

    @Test fun scheduledCompletionIsAtomicAndRepeatedCallDoesNotDuplicateMoney() = runBlocking {
        val schedules = ScheduledTransactionRepositoryImpl(database.scheduledTransactionDao(), database, session, pending, scheduler)
        val id = schedules.insertScheduledTransaction(ScheduledTransaction(firestoreId = "plan", amount = 50.25,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "Plan", scheduledDate = 100))
        val plan = schedules.getScheduledTransactionById(id)!!
        val complete = RoomScheduledCompletion(database, session, pending, scheduler, Clock.systemUTC())
        assertNotNull(complete(plan)); assertNull(complete(plan))
        assertEquals(1, repository.observeTransactions().first().size)
        assertTrue(schedules.observeScheduledTransactions().first().isEmpty())
        assertEquals(2, database.syncRecordDao().pending("A").size)
    }

    @Test fun scheduledCompletionRetainsRemotePhotoMetadataWithoutUploadingCachedFile() = runBlocking {
        val schedules = ScheduledTransactionRepositoryImpl(database.scheduledTransactionDao(), database, session, pending, scheduler)
        val id = schedules.insertScheduledTransaction(ScheduledTransaction(firestoreId = "photo-plan", amount = 50.25,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "Photo plan", scheduledDate = 100,
            photoUri = "/account/A/SYNC_cached.jpg"))
        val record = database.syncRecordDao().get("A", "scheduled_transactions", "photo-plan")!!
        val metadata = mapOf("photoStorageUrl" to "https://example.test/receipt", "photoRemoved" to false, "photoVersion" to "v2")
        database.syncRecordDao().save(record.copy(pendingPayload = SyncPayload.encode(SyncPayload.decode(record.pendingPayload!!) + metadata)))
        val complete = RoomScheduledCompletion(database, session, pending, scheduler, Clock.systemUTC())
        val completed = complete(schedules.getScheduledTransactionById(id)!!)!!
        val outgoing = SyncPayload.decode(database.syncRecordDao().get("A", "transactions", completed.firestoreId)!!.pendingPayload!!)
        assertEquals("/account/A/SYNC_cached.jpg", completed.photoUri)
        metadata.forEach { (key, expected) -> assertEquals(expected, outgoing[key]) }
        assertFalse(outgoing.containsKey("photoUri"))
        assertTrue(database.syncRecordDao().get("A", "scheduled_transactions", "photo-plan")!!.pendingDelete)
    }
}

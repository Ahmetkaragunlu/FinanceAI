package com.ahmetkaragunlu.financeai.feature.schedule.data.reminder

import com.ahmetkaragunlu.financeai.core.session.local.entity.AccountPreferences
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.sync.local.entity.SyncRecord

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.session.*
import com.ahmetkaragunlu.financeai.core.sync.*
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.ScheduledTransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.schedule.data.repository.ScheduledTransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionRemoteStore
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.notification.ReminderPresenter
import com.ahmetkaragunlu.financeai.photo.PhotoWorkScheduler
import dagger.Lazy
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class CompletedPlanEditResolutionTest {
    private lateinit var database: FinanceDatabase
    private lateinit var session: AccountSession
    private lateinit var resolution: CompletedPlanEditResolution
    private lateinit var transactionStore: TransactionRemoteStore
    private lateinit var schedules: ScheduledTransactionRepositoryImpl
    private val presenter = object : ReminderPresenter {
        override fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String) = false
        override fun cancel(ownerId: String, remoteId: String) = Unit
    }
    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                object : Worker(appContext, workerParameters) { override fun doWork(): Result = Result.success() }
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setWorkerFactory(factory).build())
        database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        database.accountDao().save(AccountPreferences("A", "USD", "UTC"))
        database.accountDao().setActive(ActiveAccountRow(ownerId = "A"))
        session = AccountSession().apply { activate("A", "USD", "UTC") }
        val work = WorkManager.getInstance(context)
        val reminderScheduler = ReminderScheduler(work, Clock.systemUTC())
        val photos = PhotoRemoteCache(context, Lazy { error("No media network in resolution tests") }, session, Dispatchers.IO)
        transactionStore = TransactionRemoteStore(database, photos)
        val scheduleStore = ScheduledTransactionRemoteStore(database, photos, reminderScheduler, presenter)
        val pending = PendingChanges(database)
        schedules = ScheduledTransactionRepositoryImpl(database.scheduledTransactionDao(), database, session, pending,
            SyncScheduler(work), reminderScheduler, presenter)
        resolution = CompletedPlanEditResolution(database, pending, transactionStore, scheduleStore,
            PhotoWorkScheduler(work, session, database))
    }
    @After fun close() { database.close(); WorkManagerTestInitHelper.closeWorkDatabase() }
    private suspend fun conflict(): SyncRecord {
        schedules.insertScheduledTransaction(ScheduledTransaction(firestoreId = "p1", amount = 50.0,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = "before", scheduledDate = 100))
        val before = database.syncRecordDao().get("A", "scheduled_transactions", "p1")!!.pendingPayload
        schedules.updateScheduledTransaction(schedules.getScheduledTransactionByFirestoreId("p1")!!.copy(amount = 75.0, note = "local"))
        val row = database.syncRecordDao().get("A", "scheduled_transactions", "p1")!!
        val conflict = row.copy(basePayload = before, conflictPayload = null, conflictRevision = 8)
        database.syncRecordDao().save(conflict)
        return conflict
    }
    private fun snapshot(amount: Long = 5000): CompletedFinancialSnapshot {
        val data = transactionStore.normalize(mapOf("amountMinor" to amount, "currencyCode" to "USD",
            "transaction" to "EXPENSE", "category" to "FOOD", "note" to "before", "date" to 9999L), session.requireAccount())
        return CompletedFinancialSnapshot("completed_p1", SyncPayload.encode(data), 7, data)
    }
    @Test fun keepLocalUpdatesOneExistingFinancialIdentityWithoutReopeningThePlan() = runBlocking {
        val conflict = conflict()
        val remote = snapshot()
        transactionStore.apply(session.requireAccount(), remote.remoteId, remote.prepared)
        val originalId = database.transactionDao().getTransactionByFirestoreId(remote.remoteId)!!.id
        session.withAccount { account -> database.withTransaction { resolution.apply(account, conflict, true, remote) } }
        val rows = database.transactionDao().getAllTransactionsOneShot()
        assertEquals(1, rows.size)
        assertEquals(originalId, rows.single().id)
        assertEquals(7500L, rows.single().amountMinor)
        assertEquals(9999L, rows.single().date)
        assertNull(schedules.getScheduledTransactionByFirestoreId("p1"))
        assertEquals("completed_p1", database.syncRecordDao().pending("A").single().remoteId)
    }
    @Test fun aNewRemoteFinancialOverlapKeepsBothVersionsForAnotherChoice() = runBlocking {
        val conflict = conflict()
        val remote = snapshot(9000)
        session.withAccount { account -> database.withTransaction { resolution.apply(account, conflict, true, remote) } }
        val row = database.syncRecordDao().get("A", "transactions", "completed_p1")!!
        assertEquals(7500L, SyncPayload.decode(row.pendingPayload!!)["amountMinor"])
        assertEquals(9000L, SyncPayload.decode(row.conflictPayload!!)["amountMinor"])
        assertEquals(7L, row.conflictRevision)
        assertNull(schedules.getScheduledTransactionByFirestoreId("p1"))
    }
}

package com.ahmetkaragunlu.financeai.feature.schedule.data.reminder

import com.ahmetkaragunlu.financeai.core.session.local.entity.AccountPreferences
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.*
import com.ahmetkaragunlu.financeai.core.sync.*
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.SharedReminderRemoteStore
import com.ahmetkaragunlu.financeai.feature.schedule.data.repository.ScheduledTransactionRepositoryImpl
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.notification.ReminderPresenter
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class ReminderCoordinatorTest {
    private lateinit var database: FinanceDatabase
    private lateinit var session: AccountSession
    private lateinit var scheduler: ReminderScheduler
    private lateinit var coordinator: ReminderCoordinator
    private lateinit var repository: ScheduledTransactionRepositoryImpl
    private val day = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()
    private var now = day + 9 * 3_600_000
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }
    private var permission = true
    private val displayed = mutableListOf<ReminderKind>()
    private val presenter = object : ReminderPresenter {
        override fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String): Boolean {
            if (permission) displayed += kind
            return permission
        }
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
        val workManager = WorkManager.getInstance(context)
        val sync = SyncScheduler(workManager)
        scheduler = ReminderScheduler(workManager, clock)
        repository = ScheduledTransactionRepositoryImpl(database.scheduledTransactionDao(), database, session,
            PendingChanges(database), sync, scheduler, presenter)
        coordinator = ReminderCoordinator(database, session, ScheduleCommandQueue(database, clock), sync, scheduler, presenter, clock)
    }
    @After fun close() { database.close(); WorkManagerTestInitHelper.closeWorkDatabase() }
    private suspend fun plan(date: Long = day) {
        repository.insertScheduledTransaction(ScheduledTransaction(firestoreId = "p1", amount = 25.25,
            type = TransactionType.EXPENSE, category = CategoryType.FOOD, note = null, scheduledDate = date))
    }
    @Test fun permissionDenialDoesNotManufactureAnExpirationReceiptOrDeleteThePlan() = runBlocking {
        plan(day - 86_400_000)
        permission = false
        coordinator.process(session.requireAccount(), "p1")
        assertNull(database.reminderStateDao().get("A", "p1")!!.expiredShownAt)
        assertTrue(database.scheduleCommandDao().forAccount("A").isEmpty())
        assertNotNull(repository.getScheduledTransactionByFirestoreId("p1"))
    }
    @Test fun repeatWakeupsAndSnoozeDoNotCreateAnHourlyNotificationLoop() = runBlocking {
        plan()
        val account = session.requireAccount()
        coordinator.process(account, "p1"); coordinator.process(account, "p1")
        assertEquals(listOf(ReminderKind.MORNING), displayed)
        now = day + 10 * 3_600_000
        assertEquals(day + 11 * 3_600_000, coordinator.snooze(account, "p1"))
        now = day + 11 * 3_600_000
        coordinator.process(account, "p1"); coordinator.process(account, "p1")
        now = day + 12 * 3_600_000
        coordinator.process(account, "p1")
        assertEquals(listOf(ReminderKind.MORNING, ReminderKind.SNOOZE), displayed)
        assertEquals(1, database.scheduleCommandDao().forAccount("A").size)
    }
    @Test fun localReceiptCannotDeleteAndSharedTerminalStateCancelsOnlyAfterRemoteAcceptance() = runBlocking {
        val date = day - 86_400_000
        plan(date)
        val record = database.syncRecordDao().get("A", "scheduled_transactions", "p1")!!
        database.syncRecordDao().save(record.copy(basePayload = record.pendingPayload, pendingPayload = null, mutationId = null))
        val account = session.requireAccount()
        coordinator.process(account, "p1")
        assertEquals("expiration_shown", database.scheduleCommandDao().forAccount("A").single().type)
        now += 48 * 3_600_000
        coordinator.process(account, "p1")
        assertNotNull(repository.getScheduledTransactionByFirestoreId("p1"))
        session.withAccount {
            database.withTransaction {
                SharedReminderRemoteStore(database, scheduler, presenter).apply(account, "p1",
                    mapOf("scheduledDate" to date, "status" to "deleted", "revision" to 7L))
            }
        }
        assertNull(repository.getScheduledTransactionByFirestoreId("p1"))
        assertFalse(database.reminderStateDao().get("A", "p1")!!.active)
    }
}

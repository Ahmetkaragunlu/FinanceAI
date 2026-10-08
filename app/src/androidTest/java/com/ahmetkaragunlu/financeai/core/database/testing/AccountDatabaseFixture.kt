package com.ahmetkaragunlu.financeai.core.database.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.local.entity.AccountPreferences
import com.ahmetkaragunlu.financeai.core.session.local.entity.ActiveAccountRow
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.sync.local.PendingChanges
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Shared Room/account fixture. Queued jobs are inert; these tests inspect durable local intent. */
class AccountDatabaseFixture : AutoCloseable {
    val context: Context = ApplicationProvider.getApplicationContext()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC)
    val session = AccountSession()
    val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
    val workManager: WorkManager
    val scheduler: SyncScheduler
    val pending = PendingChanges(database)

    init {
        val workers = object : WorkerFactory() {
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                object : Worker(context, parameters) {
                    override fun doWork(): Result = Result.success()
                }
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setWorkerFactory(workers).build()
        )
        workManager = WorkManager.getInstance(context)
        scheduler = SyncScheduler(workManager)
    }

    suspend fun activate(owner: String = "A", currency: String = "USD", zone: String = "UTC") {
        database.accountDao().save(AccountPreferences(owner, currency, zone))
        database.accountDao().setActive(ActiveAccountRow(ownerId = owner))
        session.activate(owner, currency, zone)
    }

    fun rejectSyncWrites() {
        val db = database.openHelper.writableDatabase
        db.execSQL("CREATE TRIGGER reject_test_sync_insert BEFORE INSERT ON sync_records BEGIN SELECT RAISE(ABORT, 'test outbox failure'); END")
        db.execSQL("CREATE TRIGGER reject_test_sync_update BEFORE UPDATE ON sync_records BEGIN SELECT RAISE(ABORT, 'test outbox failure'); END")
    }

    fun allowSyncWrites() {
        val db = database.openHelper.writableDatabase
        db.execSQL("DROP TRIGGER reject_test_sync_insert")
        db.execSQL("DROP TRIGGER reject_test_sync_update")
    }

    override fun close() {
        database.close()
        WorkManagerTestInitHelper.closeWorkDatabase()
    }
}

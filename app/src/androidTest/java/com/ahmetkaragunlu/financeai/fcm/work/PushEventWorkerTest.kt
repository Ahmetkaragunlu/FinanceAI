package com.ahmetkaragunlu.financeai.fcm.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.database.testing.AccountDatabaseFixture
import com.ahmetkaragunlu.financeai.core.session.testing.workerSessions
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class PushEventWorkerTest {
    private fun worker(f: AccountDatabaseFixture, engine: AccountSyncEngine, reminders: ReminderCoordinator): PushEventWorker {
        val sessions = workerSessions(f.session)
        val factory = object : WorkerFactory() {
            override fun createWorker(context: Context, name: String, parameters: WorkerParameters): ListenableWorker =
                PushEventWorker(context, parameters, sessions, f.database, engine, reminders, f.clock)
        }
        return TestListenableWorkerBuilder<PushEventWorker>(f.context).setWorkerFactory(factory).setInputData(workDataOf(
            "account_owner_id" to "A", "record" to "plan", "event" to "event", "type" to "SCHEDULE_STATE_CHANGED")).build()
    }

    @Test fun eventIsAcknowledgedOnlyAfterSyncAndReminderProcessingAndRetryIsIdempotent(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val engine = mock(AccountSyncEngine::class.java)
            val reminders = mock(ReminderCoordinator::class.java)
            doAnswer { runBlocking { assertFalse(checkNotNull(f.database.pushEventDao().get("A", "event")).handled) }; Unit }
                .`when`(reminders).process(f.session.requireAccount(), "plan")
            assertEquals(ListenableWorker.Result.success(), worker(f, engine, reminders).doWork())
            assertTrue(checkNotNull(f.database.pushEventDao().get("A", "event")).handled)
            assertEquals(ListenableWorker.Result.success(), worker(f, engine, reminders).doWork())
            verify(engine, times(1)).synchronize(f.session.requireAccount())
            verify(reminders, times(1)).process(f.session.requireAccount(), "plan")
        }
    }

    @Test fun failureAndCancellationLeaveTheReceiptUnacknowledged(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val engine = mock(AccountSyncEngine::class.java)
            val reminders = mock(ReminderCoordinator::class.java)
            doAnswer { throw IOException("synthetic offline") }.`when`(engine).synchronize(f.session.requireAccount())
            assertEquals(ListenableWorker.Result.retry(), worker(f, engine, reminders).doWork())
            assertFalse(checkNotNull(f.database.pushEventDao().get("A", "event")).handled)
            doAnswer { throw CancellationException() }.`when`(engine).synchronize(f.session.requireAccount())
            assertThrows(CancellationException::class.java) { runBlocking { worker(f, engine, reminders).doWork() } }
            assertFalse(checkNotNull(f.database.pushEventDao().get("A", "event")).handled)
        }
    }

    @Test fun accountChangeAfterSyncCannotAcknowledgeThePreviousOwnersEvent(): Unit = runBlocking {
        AccountDatabaseFixture().use { f ->
            f.activate()
            val account = f.session.requireAccount()
            val engine = mock(AccountSyncEngine::class.java)
            val reminders = mock(ReminderCoordinator::class.java)
            doAnswer { runBlocking { f.activate("B", "EUR") }; Unit }.`when`(engine).synchronize(account)
            assertEquals(ListenableWorker.Result.success(), worker(f, engine, reminders).doWork())
            assertFalse(checkNotNull(f.database.pushEventDao().get("A", "event")).handled)
            assertEquals(null, f.database.pushEventDao().get("B", "event"))
        }
    }
}

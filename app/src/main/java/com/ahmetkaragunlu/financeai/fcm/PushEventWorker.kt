package com.ahmetkaragunlu.financeai.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.PushEvent
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import kotlinx.coroutines.CancellationException

@HiltWorker
class PushEventWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val sessions: SessionCoordinator, private val database: FinanceDatabase,
    private val engine: AccountSyncEngine, private val reminders: ReminderCoordinator, private val clock: Clock
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        val record = inputData.getString(RECORD) ?: return Result.failure()
        val event = inputData.getString(EVENT) ?: return Result.failure()
        val type = inputData.getString(TYPE) ?: return Result.failure()
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == owner } ?: return Result.success()
            if (database.pushEventDao().get(owner, event)?.handled == true) return Result.success()
            sessions.session.withAccount { current ->
                check(current == account)
                database.pushEventDao().insert(PushEvent(owner, event, record, type, clock.millis()))
            }
            // FCM is an invalidation hint, never a financial command.
            engine.synchronize(account)
            if (!sessions.session.isCurrent(account)) return Result.success()
            reminders.process(account, record)
            sessions.session.withAccount { current ->
                check(current == account)
                database.pushEventDao().handled(owner, event)
                database.pushEventDao().pruneHandled(owner, clock.millis() - 30L * 24 * 60 * 60 * 1000)
            }
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
    companion object { const val RECORD = "record"; const val EVENT = "event"; const val TYPE = "type" }
}

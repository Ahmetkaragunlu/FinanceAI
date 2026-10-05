package com.ahmetkaragunlu.financeai.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Compatibility entry for old persisted jobs: re-evaluate the approved expiration policy. */
@HiltWorker
class DeleteExpiredNotification @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val sessions: SessionCoordinator,
    private val reminders: ReminderCoordinator,
    private val repository: ScheduledTransactionRepository
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == ownerId } ?: return Result.success()
            val plan = repository.getScheduledTransactionById(inputData.getLong(NotificationWorker.TRANSACTION_ID_KEY, -1))
                ?: return Result.success()
            reminders.process(account, plan.firestoreId)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
}

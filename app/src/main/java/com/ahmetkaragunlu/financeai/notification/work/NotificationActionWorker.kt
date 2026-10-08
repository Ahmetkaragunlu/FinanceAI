package com.ahmetkaragunlu.financeai.notification.work

import com.ahmetkaragunlu.financeai.notification.action.NotificationActions

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Cold-start/readiness cannot make an eight-second receiver timeout lose a user command. */
@HiltWorker
class NotificationActionWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val sessions: SessionCoordinator, private val repository: ScheduledTransactionRepository,
    private val completion: CompleteScheduledTransaction, private val reminders: ReminderCoordinator
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        val record = inputData.getString(NotificationWorker.FIRESTORE_ID_KEY) ?: return Result.failure()
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == owner } ?: return Result.success()
            when (inputData.getString(ACTION)) {
                NotificationActions.ACTION_CONFIRM -> {
                    val plan = repository.getScheduledTransactionByFirestoreId(record)
                    if (plan != null) completion(plan)
                    reminders.dismiss(account, record)
                }
                NotificationActions.ACTION_SNOOZE, NotificationActions.ACTION_CANCEL ->
                    reminders.snooze(account, record, inputData.getLong(REQUESTED_AT, -1))
                NotificationActions.ACTION_DISMISS -> reminders.dismiss(account, record)
                else -> return Result.failure()
            }
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
    companion object { const val ACTION = "action"; const val REQUESTED_AT = "requested_at" }
}

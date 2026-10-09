package com.ahmetkaragunlu.financeai.notification.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKeys
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Re-evaluates the plan's expiration policy through the shared reminder coordinator. */
@HiltWorker
class DeleteExpiredNotification @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val sessions: SessionCoordinator,
    private val reminders: ReminderCoordinator,
    private val repository: ScheduledTransactionRepository
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(AccountWork.OWNER_ID) ?: return Result.failure()
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == ownerId } ?: return Result.success()
            val plan = repository.getScheduledTransactionById(inputData.getLong(ReminderKeys.LOCAL_ID, -1))
                ?: return Result.success()
            reminders.process(account, plan.firestoreId)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
}

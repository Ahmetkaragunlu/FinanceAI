package com.ahmetkaragunlu.financeai.notification.work

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

/** Runs account-scoped reminder decisions through the shared coordinator. */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val sessions: SessionCoordinator,
    private val reminders: ReminderCoordinator,
    private val repository: ScheduledTransactionRepository
) : CoroutineWorker(context, parameters) {
    companion object {
        const val CHANNEL_ID = "scheduled_transaction_channel"
        const val TRANSACTION_ID_KEY = "transaction_id"
        const val FIRESTORE_ID_KEY = "firestore_id"
    }
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == ownerId } ?: return Result.success()
            val remoteId = inputData.getString(FIRESTORE_ID_KEY)
                ?: repository.getScheduledTransactionById(inputData.getLong(TRANSACTION_ID_KEY, -1))?.firestoreId
            if (remoteId != null) reminders.process(account, remoteId)
            else reminders.restoreCurrent()
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
}

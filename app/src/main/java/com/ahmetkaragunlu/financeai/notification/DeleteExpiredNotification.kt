package com.ahmetkaragunlu.financeai.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class DeleteExpiredNotification @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val session: AccountSession,
    private val repository: ScheduledTransactionRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.success()
        if (session.account.value?.ownerId != ownerId) return Result.success()
        return try {
            val transactionId = inputData.getLong(NotificationWorker.TRANSACTION_ID_KEY, -1L)
            if (transactionId == -1L) {
                return Result.failure()
            }
            val transaction = repository.getScheduledTransactionById(transactionId)
            if (transaction != null) {
                repository.deleteScheduledTransaction(transaction)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure()
        }
    }
}

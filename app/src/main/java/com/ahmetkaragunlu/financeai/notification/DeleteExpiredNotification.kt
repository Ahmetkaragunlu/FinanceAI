package com.ahmetkaragunlu.financeai.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DeleteExpiredNotification @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val transactionId = inputData.getLong(NotificationWorker.TRANSACTION_ID_KEY, -1L)
            if (transactionId == -1L) {
                return Result.failure()
            }
            val transaction = repository.getScheduledTransactionById(transactionId)
            if (transaction != null) {
                if (transaction.firestoreId.isNotEmpty()) {
                    val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                        firestoreId = transaction.firestoreId
                    )
                    if (deleteResult.isFailure) {
                        return Result.failure()
                    }
                }
                repository.deleteScheduledTransaction(transaction)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
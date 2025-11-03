package com.ahmetkaragunlu.financeai.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.firebaserepo.FirebaseSyncService
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DeleteExpiredTransactionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DeleteExpiredWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val transactionId = inputData.getLong(NotificationWorker.TRANSACTION_ID_KEY, -1L)
            if (transactionId == -1L) {
                return Result.failure()
            }

            Log.d(TAG, "Deleting expired transaction - Local ID: $transactionId")

            // Local ID ile transaction'ı bul
            val transaction = repository.getScheduledTransactionById(transactionId)

            if (transaction != null) {
                Log.d(TAG, "Found transaction - Firestore ID: ${transaction.firestoreId}")

                // 1. Firebase'den sil
                if (transaction.firestoreId.isNotEmpty()) {
                    val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                        transaction.firestoreId
                    )
                    if (deleteResult.isSuccess) {
                        Log.d(TAG, "Successfully deleted from Firebase")
                    } else {
                        Log.e(TAG, "Failed to delete from Firebase", deleteResult.exceptionOrNull())
                    }
                }

                // 2. Local'den sil
                repository.deleteScheduledTransaction(transaction)
                Log.d(TAG, "Deleted from local DB")

                Result.success()
            } else {
                Log.w(TAG, "Transaction not found with ID: $transactionId")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expired transaction", e)
            Result.failure()
        }
    }
}
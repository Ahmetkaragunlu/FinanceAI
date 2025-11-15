
package com.ahmetkaragunlu.financeai.notification

import android.content.Context
import android.util.Log
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

    companion object {
        private const val TAG = "DeleteExpiredWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val transactionId = inputData.getLong(NotificationWorker.TRANSACTION_ID_KEY, -1L)
            if (transactionId == -1L) {
                return Result.failure()
            }

            Log.d(TAG, "═══════════════════════════════════════════════════")
            Log.d(TAG, "⏰ Deleting expired scheduled transaction")
            Log.d(TAG, "Local ID: $transactionId")

            val transaction = repository.getScheduledTransactionById(transactionId)

            if (transaction != null) {
                Log.d(TAG, "Found expired transaction - Firestore ID: ${transaction.firestoreId}")
                if (transaction.firestoreId.isNotEmpty()) {
                    Log.d(TAG, "Deleting from Firebase...")

                    val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                        transaction.firestoreId
                    )

                    if (deleteResult.isSuccess) {
                        Log.d(TAG, "✅ Successfully deleted from Firebase")
                        Log.d(TAG, "✅ CANCEL_NOTIFICATION sent to all devices")
                        Log.d(TAG, "✅ Firestore + Storage photo deleted")
                    } else {
                        Log.e(TAG, "❌ Failed to delete from Firebase", deleteResult.exceptionOrNull())
                        return Result.failure()
                    }
                } else {
                    Log.w(TAG, "⚠️ No Firestore ID, only local delete")
                }

                repository.deleteScheduledTransaction(transaction)
                Log.d(TAG, "✅ Deleted from local DB")
                Log.d(TAG, "═══════════════════════════════════════════════════")

                Result.success()
            } else {
                Log.w(TAG, "⚠️ Expired transaction not found with ID: $transactionId")
                Log.d(TAG, "═══════════════════════════════════════════════════")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting expired transaction", e)
            Log.d(TAG, "═══════════════════════════════════════════════════")
            Result.failure()
        }
    }
}
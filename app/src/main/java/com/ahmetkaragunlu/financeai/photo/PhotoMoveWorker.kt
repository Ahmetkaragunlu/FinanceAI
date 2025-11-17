package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class PhotoMoveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoStorageManager: PhotoStorageManager,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SCHEDULED_ID = "scheduled_id"
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val KEY_LOCAL_PATH = "local_path"
    }

    override suspend fun doWork(): Result {
        val scheduledId = inputData.getString(KEY_SCHEDULED_ID) ?: return Result.failure()
        val transactionId = inputData.getString(KEY_TRANSACTION_ID) ?: return Result.failure()
        val localPath = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()

        return try {
            val uploadResult = photoStorageManager.uploadTransactionPhoto(localPath, transactionId)
            if (uploadResult.isSuccess) {
                val newPhotoUrl = uploadResult.getOrNull()
                if (!newPhotoUrl.isNullOrBlank()) {
                    firestore.collection("transactions")
                        .document(transactionId)
                        .update("photoStorageUrl", newPhotoUrl)
                        .await()
                }
                try {
                    photoStorageManager.deleteScheduledPhotoById(scheduledId)
                } catch (e: Exception) {
                    Log.w("PhotoMoveWorker", "Eski fotoğraf silinemedi veya zaten yok: ${e.message}")
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
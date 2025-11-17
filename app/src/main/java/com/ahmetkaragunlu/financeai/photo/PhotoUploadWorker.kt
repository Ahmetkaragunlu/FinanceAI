package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class PhotoUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoStorageManager: PhotoStorageManager,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_FIRESTORE_ID = "firestore_id"
        const val KEY_COLLECTION_TYPE = "collection_type" // "transactions" veya "scheduled"
    }

    override suspend fun doWork(): Result {
        val localPath = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()
        val firestoreId = inputData.getString(KEY_FIRESTORE_ID) ?: return Result.failure()
        val collectionType = inputData.getString(KEY_COLLECTION_TYPE) ?: "transactions"

        return try {
            // 1. Fotoğrafı Storage'a yükle
            val uploadResult = if (collectionType == "scheduled") {
                photoStorageManager.uploadScheduledPhoto(localPath, firestoreId)
            } else {
                photoStorageManager.uploadTransactionPhoto(localPath, firestoreId)
            }

            if (uploadResult.isSuccess) {
                val downloadUrl = uploadResult.getOrNull()

                // 2. Firestore belgesini güncelle (URL'i ekle)
                val collectionPath = if (collectionType == "scheduled") "scheduled_transactions" else "transactions"

                firestore.collection(collectionPath)
                    .document(firestoreId)
                    .update("photoStorageUrl", downloadUrl)
                    .await()

                Log.d("PhotoUploadWorker", "Photo uploaded and Firestore updated successfully.")
                Result.success()
            } else {
                Log.e("PhotoUploadWorker", "Upload failed", uploadResult.exceptionOrNull())
                Result.retry() // Hata olursa (örn. ağ koparsa) tekrar dene
            }
        } catch (e: Exception) {
            Log.e("PhotoUploadWorker", "Error in worker", e)
            Result.retry()
        }
    }
}
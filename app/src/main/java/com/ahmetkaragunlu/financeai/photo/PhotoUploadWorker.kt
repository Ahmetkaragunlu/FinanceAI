package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
        const val KEY_COLLECTION_TYPE = "collection_type"
    }

    override suspend fun doWork(): Result {
        val localPath = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()
        val firestoreId = inputData.getString(KEY_FIRESTORE_ID) ?: return Result.failure()
        val collectionType = inputData.getString(KEY_COLLECTION_TYPE) ?: "transactions"
        val collectionPath = if (collectionType == "scheduled") "scheduled_transactions" else "transactions"

        return try {
            try {
                val docSnapshot = firestore.collection(collectionPath).document(firestoreId).get().await()
                if (!docSnapshot.exists()) {
                    return Result.success()
                }
            } catch (e: Exception) {
                return Result.retry()
            }
            val uploadResult = if (collectionType == "scheduled") {
                photoStorageManager.uploadScheduledPhoto(localPath, firestoreId)
            } else {
                photoStorageManager.uploadTransactionPhoto(localPath, firestoreId)
            }

            if (uploadResult.isSuccess) {
                val downloadUrl = uploadResult.getOrNull()
                try {
                    firestore.collection(collectionPath)
                        .document(firestoreId)
                        .update("photoStorageUrl", downloadUrl)
                        .await()
                    Result.success()
                } catch (e: Exception) {
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                        if (!downloadUrl.isNullOrBlank()) {
                            photoStorageManager.deletePhoto(downloadUrl)
                        }
                        Result.success()
                    } else {
                        throw e
                    }
                }
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
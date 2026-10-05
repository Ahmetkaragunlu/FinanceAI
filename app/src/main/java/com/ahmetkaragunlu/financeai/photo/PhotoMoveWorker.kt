package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@HiltWorker
class PhotoMoveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoStorageManager: PhotoStorageManager,
    private val session: AccountSession,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SCHEDULED_ID = "scheduled_id"
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val KEY_LOCAL_PATH = "local_path"
    }

    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        val account = session.account.value?.takeIf { it.ownerId == ownerId } ?: return Result.success()
        val scheduledId = inputData.getString(KEY_SCHEDULED_ID) ?: return Result.failure()
        val transactionId = inputData.getString(KEY_TRANSACTION_ID) ?: return Result.failure()
        val localPath = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()

        return try {
            if (!session.isCurrent(account)) return Result.success()
            val document = firestore.collection("transactions").document(transactionId).get().await()
            if (!document.exists()) return Result.retry()
            if (document.getString("userId") != ownerId || document.getBoolean("deleted") == true) return Result.success()
            val uploadResult = photoStorageManager.uploadTransactionPhoto(localPath, transactionId, ownerId)
            if (!session.isCurrent(account)) return Result.success()
            if (uploadResult.isSuccess) {
                val newPhotoUrl = uploadResult.getOrNull()
                if (!newPhotoUrl.isNullOrBlank()) {
                    firestore.runTransaction { transaction ->
                        val ref = firestore.collection("transactions").document(transactionId)
                        val current = transaction.get(ref)
                        check(session.isCurrent(account))
                        if (current.getString("userId") != ownerId || current.getBoolean("deleted") == true) return@runTransaction
                        transaction.update(ref, mapOf("photoStorageUrl" to newPhotoUrl, "photoRemoved" to false, "photoVersion" to id.toString(), "revision" to ((current.getLong("revision") ?: 0L) + 1)))
                    }.await()
                }
                try {
                    photoStorageManager.deleteScheduledPhotoById(scheduledId, ownerId)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w("PhotoMoveWorker", "Eski fotoğraf silinemedi veya zaten yok: ${e.javaClass.simpleName}")
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }
}

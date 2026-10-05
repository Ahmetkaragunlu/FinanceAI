package com.ahmetkaragunlu.financeai.photo

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@HiltWorker
class PhotoUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoStorageManager: PhotoStorageManager,
    private val session: AccountSession,
    private val database: FinanceDatabase,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_FIRESTORE_ID = "firestore_id"
        const val KEY_COLLECTION_TYPE = "collection_type"
    }

    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        val account = session.account.value?.takeIf { it.ownerId == ownerId } ?: return Result.success()
        val localPath = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()
        val firestoreId = inputData.getString(KEY_FIRESTORE_ID) ?: return Result.failure()
        val collectionType = inputData.getString(KEY_COLLECTION_TYPE) ?: "transactions"
        val collectionPath = if (collectionType == "scheduled") "scheduled_transactions" else "transactions"

        return try {
            try {
                val docSnapshot = firestore.collection(collectionPath).document(firestoreId).get().await()
                if (!docSnapshot.exists()) {
                    val sync = database.syncRecordDao().get(ownerId, collectionPath, firestoreId)
                    return if (sync?.basePayload == null && sync?.mutationId == null && sync != null) Result.success() else Result.retry()
                }
                if (docSnapshot.getString("userId") != ownerId || docSnapshot.getBoolean("deleted") == true) return Result.success()
                if (!session.isCurrent(account)) return Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return Result.retry()
            }
            if (!session.isCurrent(account)) return Result.success()
            val local = if (collectionType == "scheduled") database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(firestoreId)?.photoUri else database.transactionDao().getTransactionByFirestoreId(firestoreId)?.photoUri
            if (local != localPath) return Result.success()
            val uploadResult = if (collectionType == "scheduled") {
                photoStorageManager.uploadScheduledPhoto(localPath, firestoreId, ownerId)
            } else {
                photoStorageManager.uploadTransactionPhoto(localPath, firestoreId, ownerId)
            }

            if (!session.isCurrent(account)) return Result.success()
            val latestLocal = if (collectionType == "scheduled") database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(firestoreId)?.photoUri else database.transactionDao().getTransactionByFirestoreId(firestoreId)?.photoUri
            if (!session.isCurrent(account) || latestLocal != localPath) return Result.success()
            if (uploadResult.isSuccess) {
                val downloadUrl = uploadResult.getOrNull()
                try {
                    firestore.runTransaction { transaction ->
                        val ref = firestore.collection(collectionPath).document(firestoreId)
                        val current = transaction.get(ref)
                        check(session.isCurrent(account))
                        if (current.getString("userId") != ownerId || current.getBoolean("deleted") == true) return@runTransaction
                        transaction.update(ref, mapOf("photoStorageUrl" to downloadUrl, "photoRemoved" to false, "photoVersion" to id.toString(), "revision" to ((current.getLong("revision") ?: 0L) + 1)))
                    }.await()
                    Result.success()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
            Result.retry()
        }
    }
}

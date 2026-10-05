package com.ahmetkaragunlu.financeai.photo

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.sync.contract.SyncFields
import com.ahmetkaragunlu.financeai.core.media.PhotoFields

import android.content.Context
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ScheduleCommandType
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoLocalStore
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

@HiltWorker
class PhotoUploadWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted parameters: WorkerParameters,
    private val storage: PhotoStorageManager, private val sessions: SessionCoordinator,
    private val database: FinanceDatabase, private val firestore: FirebaseFirestore,
    private val files: PhotoLocalStore
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString(SyncScheduler.OWNER_ID) ?: return Result.failure()
        val path = inputData.getString(KEY_LOCAL_PATH) ?: return Result.failure()
        val record = inputData.getString(KEY_FIRESTORE_ID) ?: return Result.failure()
        val collection = inputData.getString(KEY_COLLECTION_TYPE) ?: FirestoreCollections.TRANSACTIONS
        if (collection !in setOf(FirestoreCollections.TRANSACTIONS, "scheduled")) return Result.failure()
        val version = inputData.getString(KEY_VERSION) ?: File(path).nameWithoutExtension
        val remoteCollection = if (collection == "scheduled") FirestoreCollections.SCHEDULED_TRANSACTIONS else collection
        suspend fun localPath(): String? = if (collection == "scheduled")
            database.scheduledTransactionDao().getScheduledTransactionByFirestoreId(record)?.photoUri
            else database.transactionDao().getTransactionByFirestoreId(record)?.photoUri
        suspend fun retire(): Result {
            database.photoOperationDao().acknowledge(owner, collection, record, path)
            files.delete(path) // Reference guard preserves the current row/other pending operation.
            return Result.success()
        }
        return try {
            sessions.prepare()
            val account = sessions.session.account.value?.takeIf { it.ownerId == owner } ?: return Result.success()
            if (localPath() != path) return retire()
            val operation = database.photoOperationDao().forAccount(owner)
                .firstOrNull { it.collection == collection && it.remoteId == record && it.path == path }
            if (operation?.failure != null) return Result.failure()
            if (!File(path).isFile) {
                database.photoOperationDao().fail(owner, collection, record, path, "missing_file")
                return Result.failure()
            }
            val ref = firestore.collection(remoteCollection).document(record)
            val initial = ref.get(Source.SERVER).await()
            if (!initial.exists()) {
                val sync = database.syncRecordDao().get(owner, remoteCollection, record)
                val commands = database.scheduleCommandDao().forAccount(owner)
                val command = commands.firstOrNull { ScheduleCommandType.fromWire(it.type) == ScheduleCommandType.COMPLETE && "completed_${it.remoteId}" == record }
                if (sync?.permanentFailure == true || command?.failure != null || (sync == null && command == null)) {
                    database.photoOperationDao().fail(owner, collection, record, path, "remote_record_unavailable")
                    return Result.failure()
                }
                return Result.retry() // Required remote upsert/complete is durably pending, not silently successful.
            }
            if (initial.getString(SyncFields.USER_ID) != owner || initial.getBoolean(SyncFields.DELETED) == true) return retire()
            if (initial.getString(PhotoFields.VERSION) == version) return retire()
            if (initial.getString(PhotoFields.INTENT) != null && initial.getString(PhotoFields.INTENT) != version) return Result.retry()
            val url = storage.uploadPhoto(path, record, collection, owner, version)
            if (!sessions.session.isCurrent(account)) return Result.success()
            if (localPath() != path) { storage.deletePhoto(url, owner); return retire() }
            val attached = firestore.runTransaction { transaction ->
                val current = transaction.get(ref)
                check(sessions.session.isCurrent(account))
                if (!current.exists() || current.getString(SyncFields.USER_ID) != owner || current.getBoolean(SyncFields.DELETED) == true)
                    return@runTransaction false
                if (current.getString(PhotoFields.INTENT) != null && current.getString(PhotoFields.INTENT) != version)
                    return@runTransaction false
                if (current.getString(PhotoFields.VERSION) == version) return@runTransaction true
                transaction.update(ref, mapOf(PhotoFields.STORAGE_URL to url, PhotoFields.REMOVED to false,
                    PhotoFields.INTENT to version, PhotoFields.VERSION to version,
                    SyncFields.REVISION to ((current.getLong(SyncFields.REVISION) ?: 0L) + 1)))
                true
            }.await()
            if (!attached) storage.deletePhoto(url, owner)
            if (attached || localPath() != path) retire() else Result.retry()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            val permanent = e is IllegalArgumentException || (e is FirebaseFirestoreException &&
                e.code in setOf(FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.INVALID_ARGUMENT)) ||
                (e is StorageException && e.errorCode in setOf(StorageException.ERROR_NOT_AUTHORIZED, StorageException.ERROR_QUOTA_EXCEEDED))
            if (permanent) {
                database.photoOperationDao().fail(owner, collection, record, path, e.javaClass.simpleName)
                Result.failure()
            } else Result.retry()
        }
    }
    companion object {
        const val KEY_LOCAL_PATH = "local_path"
        const val KEY_FIRESTORE_ID = "firestore_id"
        const val KEY_COLLECTION_TYPE = "collection_type"
        const val KEY_VERSION = "version"
    }
}

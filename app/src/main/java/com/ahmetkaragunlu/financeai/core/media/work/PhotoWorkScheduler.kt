package com.ahmetkaragunlu.financeai.core.media.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.media.PhotoRecordType
import com.ahmetkaragunlu.financeai.core.media.local.entity.PhotoOperation
import com.ahmetkaragunlu.financeai.core.media.remote.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.ahmetkaragunlu.financeai.photo.PhotoUploadWorker
import java.io.File
import javax.inject.Inject

class PhotoWorkScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val session: AccountSession,
    private val database: FinanceDatabase
) {
    suspend fun upload(
        ownerId: String,
        recordType: PhotoRecordType,
        remoteId: String,
        path: String?
    ) =
        upload(ownerId, recordType.wireValue, remoteId, path)

    // Retained operations cross a String boundary; reject unknown wire values after the same guards.
    suspend fun upload(ownerId: String, collection: String, remoteId: String, path: String?) {
        if (session.account.value?.ownerId != ownerId || path.isNullOrBlank() || path.startsWith("http") || PhotoRemoteCache.isCachedPath(
                path
            )
        ) return
        requireNotNull(PhotoRecordType.fromWire(collection))
        val version = File(path).nameWithoutExtension
        database.photoOperationDao()
            .insert(PhotoOperation(ownerId, collection, remoteId, path, version))
        val request = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
            .setInputData(
                workDataOf(
                    AccountWork.OWNER_ID to ownerId,
                    PhotoUploadWorker.KEY_LOCAL_PATH to path,
                    PhotoUploadWorker.KEY_FIRESTORE_ID to remoteId,
                    PhotoUploadWorker.KEY_COLLECTION_TYPE to collection,
                    PhotoUploadWorker.KEY_VERSION to version
                )
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .addTag(AccountWork.tag(ownerId)).build()
        workManager.enqueueUniqueWork(
            "photo_${ownerId}_${collection}_${remoteId}_$version",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

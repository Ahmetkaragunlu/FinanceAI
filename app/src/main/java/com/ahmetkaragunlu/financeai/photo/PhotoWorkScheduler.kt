package com.ahmetkaragunlu.financeai.photo

import androidx.work.*
import com.ahmetkaragunlu.financeai.core.media.PhotoRemoteCache
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import javax.inject.Inject

/** All callers use the same account inputs, ordering and cancellation tag. */
class PhotoWorkScheduler @Inject constructor(private val workManager: WorkManager, private val session: AccountSession) {
    fun upload(ownerId: String, collection: String, remoteId: String, path: String?) {
        if (session.account.value?.ownerId != ownerId || path.isNullOrBlank() || path.startsWith("https://") || PhotoRemoteCache.isCachedPath(path)) return
        val request = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
            .setInputData(workDataOf(SyncScheduler.OWNER_ID to ownerId,
                PhotoUploadWorker.KEY_LOCAL_PATH to path, PhotoUploadWorker.KEY_FIRESTORE_ID to remoteId,
                PhotoUploadWorker.KEY_COLLECTION_TYPE to collection))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("account_$ownerId").build()
        workManager.enqueueUniqueWork("photo_${ownerId}_${collection}_$remoteId", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}

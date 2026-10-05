package com.ahmetkaragunlu.financeai.core.sync

import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {
    fun enqueue(ownerId: String) {
        val request = OneTimeWorkRequestBuilder<AccountSyncWorker>()
            .setInputData(workDataOf(OWNER_ID to ownerId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(tag(ownerId)).build()
        workManager.enqueueUniqueWork(tag(ownerId), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
    fun stop(ownerId: String) { workManager.cancelAllWorkByTag(tag(ownerId)) }
    companion object {
        const val OWNER_ID = "account_owner_id"
        fun tag(ownerId: String) = "account_sync_$ownerId"
    }
}

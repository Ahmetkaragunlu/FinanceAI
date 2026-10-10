package com.ahmetkaragunlu.financeai.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {
    fun enqueue(ownerId: String) {
        val request = OneTimeWorkRequestBuilder<AccountSyncWorker>()
            .setInputData(workDataOf(AccountWork.OWNER_ID to ownerId))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(tag(ownerId)).build()
        workManager.enqueueUniqueWork(tag(ownerId), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun stop(ownerId: String) {
        workManager.cancelAllWorkByTag(tag(ownerId))
    }

    companion object {
        fun tag(ownerId: String) = "account_sync_$ownerId"
    }
}

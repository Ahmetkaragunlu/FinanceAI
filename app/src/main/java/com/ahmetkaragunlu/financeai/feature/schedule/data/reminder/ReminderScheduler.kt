package com.ahmetkaragunlu.financeai.feature.schedule.data.reminder

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.notification.work.NotificationWorker
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReminderScheduler @Inject constructor(private val workManager: WorkManager, private val clock: Clock) {
    fun wake(ownerId: String, remoteId: String, at: Long = clock.millis()) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(workDataOf(SyncScheduler.OWNER_ID to ownerId, NotificationWorker.FIRESTORE_ID_KEY to remoteId))
            .setInitialDelay((at - clock.millis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .addTag(tag(ownerId, remoteId)).addTag("account_$ownerId").build()
        // Different due times cannot cancel their currently running predecessor.
        workManager.enqueueUniqueWork("${tag(ownerId, remoteId)}_$at", ExistingWorkPolicy.KEEP, request)
    }
    fun cancel(ownerId: String, remoteId: String, localId: Long? = null) {
        workManager.cancelAllWorkByTag(tag(ownerId, remoteId))
        if (localId != null) {
            workManager.cancelAllWorkByTag("scheduled_notification_$localId")
            workManager.cancelAllWorkByTag("delete_expired_$localId")
        }
    }
    companion object { fun tag(ownerId: String, remoteId: String) = "reminder_${ownerId}_$remoteId" }
}

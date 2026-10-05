package com.ahmetkaragunlu.financeai.fcm

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.Constraints
import androidx.work.NetworkType
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Callbacks hand off durable work; no network coroutine is tied to the service lifetime. */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var workManager: WorkManager
    override fun onNewToken(token: String) {
        if (token.isBlank()) return
        val work = OneTimeWorkRequestBuilder<TokenRegistrationWorker>()
            .setInputData(workDataOf(TokenRegistrationWorker.TOKEN to token)).build()
        workManager.enqueueUniqueWork("fcm_supplied_token", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayload.parse(message.data, message.messageId) ?: return
        val work = OneTimeWorkRequestBuilder<PushEventWorker>().setInputData(workDataOf(
            SyncScheduler.OWNER_ID to payload.ownerId, PushEventWorker.RECORD to payload.remoteId,
            PushEventWorker.EVENT to payload.eventId, PushEventWorker.TYPE to payload.type))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("account_${payload.ownerId}").build()
        workManager.enqueueUniqueWork("fcm_${payload.ownerId}_${payload.eventId}", ExistingWorkPolicy.KEEP, work)
    }
}

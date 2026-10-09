package com.ahmetkaragunlu.financeai.fcm.service

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.ahmetkaragunlu.financeai.fcm.PushPayload
import com.ahmetkaragunlu.financeai.fcm.work.PushEventWorker
import com.ahmetkaragunlu.financeai.fcm.work.TokenRegistrationWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Callbacks hand off durable work; no network coroutine is tied to the service lifetime. */
@AndroidEntryPoint
class FinanceMessagingService : FirebaseMessagingService() {
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
            AccountWork.OWNER_ID to payload.ownerId, PushEventWorker.RECORD to payload.remoteId,
            PushEventWorker.EVENT to payload.eventId, PushEventWorker.TYPE to payload.type))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(AccountWork.tag(payload.ownerId)).build()
        workManager.enqueueUniqueWork("fcm_${payload.ownerId}_${payload.eventId}", ExistingWorkPolicy.KEEP, work)
    }
}

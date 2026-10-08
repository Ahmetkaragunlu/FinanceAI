package com.ahmetkaragunlu.financeai.notification.action

import com.ahmetkaragunlu.financeai.notification.work.NotificationActionWorker
import com.ahmetkaragunlu.financeai.notification.work.NotificationWorker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var clock: Clock
    override fun onReceive(context: Context, intent: Intent) {
        val owner = intent.getStringExtra(SyncScheduler.OWNER_ID) ?: return
        val record = intent.getStringExtra(NotificationWorker.FIRESTORE_ID_KEY)?.takeIf { it.isNotBlank() } ?: return
        val action = intent.action?.takeIf(NotificationActions::isSupported) ?: return
        val at = clock.millis()
        val work = OneTimeWorkRequestBuilder<NotificationActionWorker>().setInputData(workDataOf(
            SyncScheduler.OWNER_ID to owner, NotificationWorker.FIRESTORE_ID_KEY to record,
            NotificationActionWorker.ACTION to action, NotificationActionWorker.REQUESTED_AT to at))
            .addTag("account_$owner").build()
        workManager.enqueueUniqueWork("reminder_action_${owner}_${record}_${action}_$at", ExistingWorkPolicy.KEEP, work)
    }
}

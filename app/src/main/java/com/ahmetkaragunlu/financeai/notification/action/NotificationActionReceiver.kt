package com.ahmetkaragunlu.financeai.notification.action

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKeys
import com.ahmetkaragunlu.financeai.notification.work.NotificationActionWorker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var clock: Clock
    override fun onReceive(context: Context, intent: Intent) {
        val owner = intent.getStringExtra(AccountWork.OWNER_ID) ?: return
        val record =
            intent.getStringExtra(ReminderKeys.REMOTE_ID)?.takeIf { it.isNotBlank() } ?: return
        val action = intent.action?.takeIf(NotificationActions::isSupported) ?: return
        // Keep the original string: legacy CANCEL jobs retain their input and unique work identity.
        val at = clock.millis()
        val work = OneTimeWorkRequestBuilder<NotificationActionWorker>().setInputData(
            workDataOf(
                AccountWork.OWNER_ID to owner, ReminderKeys.REMOTE_ID to record,
                ReminderKeys.ACTION to action, ReminderKeys.REQUESTED_AT to at
            )
        )
            .addTag(AccountWork.tag(owner)).build()
        workManager.enqueueUniqueWork(
            "reminder_action_${owner}_${record}_${action}_$at",
            ExistingWorkPolicy.KEEP,
            work
        )
    }
}

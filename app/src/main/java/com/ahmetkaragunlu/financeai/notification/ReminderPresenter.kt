package com.ahmetkaragunlu.financeai.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.formatAsCurrency
import com.ahmetkaragunlu.financeai.core.format.formatAsShortDate
import com.ahmetkaragunlu.financeai.core.navigation.FinanceLinkContract
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderKind
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.format.toResId
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject

interface ReminderPresenter {
    fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String): Boolean

    fun cancel(ownerId: String, remoteId: String)
}

class AndroidReminderPresenter
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val session: AccountSession,
    private val auth: FirebaseAuth,
) : ReminderPresenter {
    override fun show(plan: ScheduledTransaction, kind: ReminderKind, eventId: String): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val account = session.account.value?.takeIf { it.ownerId == plan.ownerId } ?: return false
        if (auth.currentUser?.uid != account.ownerId) return false
        if (
            !NotificationManagerCompat.from(context).areNotificationsEnabled() ||
                manager.getNotificationChannel(NotificationWorker.CHANNEL_ID)?.importance ==
                    NotificationManager.IMPORTANCE_NONE
        )
            return false
        val expired = kind == ReminderKind.EXPIRED
        val notificationId = if (expired) 1 else 0
        if (
            manager.activeNotifications.any {
                it.tag == tag(plan.ownerId, plan.firestoreId) &&
                    it.id == notificationId &&
                    it.notification.extras.getString(EVENT_KEY) == eventId
            }
        )
            return true
        val income = plan.type == TransactionType.INCOME
        val title =
            when {
                expired && income -> R.string.notification_income_expired_title
                expired -> R.string.notification_expense_expired_title
                income -> R.string.notification_income_title
                else -> R.string.notification_expense_title
            }
        val message =
            when {
                expired && income -> R.string.notification_income_expired_message
                expired -> R.string.notification_expense_expired_message
                income -> R.string.notification_income_message
                else -> R.string.notification_expense_message
            }
        val text =
            context.getString(
                message,
                plan.amount.formatAsCurrency(plan.currencyCode),
                context.getString(plan.category.toResId()),
                plan.scheduledDate.formatAsShortDate(zone = ZoneId.of(account.timeZoneId)),
            )
        val open =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data =
                    FinanceLinkContract.SCHEDULE_URI.toUri()
                        .buildUpon()
                        .appendQueryParameter(FinanceLinkContract.OWNER_QUERY, plan.ownerId)
                        .appendQueryParameter(FinanceLinkContract.RECORD_QUERY, plan.firestoreId)
                        .build()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(SyncScheduler.OWNER_ID, plan.ownerId)
            }
        val builder =
            NotificationCompat.Builder(context, NotificationWorker.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setExtras(Bundle().apply { putString(EVENT_KEY, eventId) })
                .setPriority(
                    if (expired) NotificationCompat.PRIORITY_DEFAULT
                    else NotificationCompat.PRIORITY_HIGH
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )
        if (!expired) {
            builder
                .setOngoing(false)
                .setDeleteIntent(action(plan, NotificationActions.ACTION_DISMISS))
                .addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.notification_action_yes),
                    action(plan, NotificationActions.ACTION_CONFIRM),
                )
                .addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.notification_action_no),
                    action(plan, NotificationActions.ACTION_SNOOZE),
                )
        }
        return try {
            if (auth.currentUser?.uid != account.ownerId) return false
            manager.notify(tag(plan.ownerId, plan.firestoreId), notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun action(plan: ScheduledTransaction, action: String): PendingIntent {
        val intent =
            Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
                // URI identity avoids PendingIntent collisions between accounts/records/actions.
                data =
                    Uri.Builder()
                        .scheme("financeai-internal")
                        .authority("reminder")
                        .appendPath(plan.ownerId)
                        .appendPath(plan.firestoreId)
                        .appendPath(action)
                        .build()
                putExtra(SyncScheduler.OWNER_ID, plan.ownerId)
                putExtra(NotificationWorker.FIRESTORE_ID_KEY, plan.firestoreId)
            }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun cancel(ownerId: String, remoteId: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(tag(ownerId, remoteId), 0)
        manager.cancel(tag(ownerId, remoteId), 1)
        // Cancel notifications from persisted pre-refactor workers as well.
        manager.cancel(remoteId.hashCode())
        manager.cancel(remoteId.hashCode() + 20000)
    }

    companion object {
        private const val EVENT_KEY = "financeai.reminder.event"

        private fun tag(ownerId: String, remoteId: String) = "reminder_$ownerId/$remoteId"
    }
}

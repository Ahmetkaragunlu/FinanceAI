package com.ahmetkaragunlu.financeai.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ahmetkaragunlu.financeai.core.coroutines.di.ApplicationScope
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.session.ActiveAccount
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.fcm.FCMNotificationSender
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_CONFIRM = "com.ahmetkaragunlu.financeai.ACTION_CONFIRM"
        const val ACTION_CANCEL = "com.ahmetkaragunlu.financeai.ACTION_CANCEL"
    }

    @Inject
    lateinit var scheduledTransactionRepository: ScheduledTransactionRepository

    @Inject
    lateinit var fcmNotificationSender: FCMNotificationSender

    @Inject lateinit var completion: CompleteScheduledTransaction
    @Inject lateinit var session: AccountSession
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val ownerId = intent.getStringExtra(SyncScheduler.OWNER_ID) ?: return
        val account = session.account.value?.takeIf { it.ownerId == ownerId } ?: return
        val firestoreId = intent.getStringExtra(NotificationWorker.FIRESTORE_ID_KEY)
        if (firestoreId.isNullOrBlank()) return
        val pendingResult = goAsync()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(firestoreId.hashCode())
        notificationManager.cancel(firestoreId.hashCode() + 20000)
        val action = intent.action
        when (action) {
            ACTION_CONFIRM, ACTION_CANCEL -> handleAction(action, firestoreId, account, pendingResult)
            else -> pendingResult.finish()
        }
    }

    private fun handleAction(action: String, firestoreId: String, account: ActiveAccount, pendingResult: PendingResult) {
        scope.launch {
            try {
                withTimeout(8_000) {
                    if (!session.isCurrent(account)) return@withTimeout
                    val plan = scheduledTransactionRepository.getScheduledTransactionByFirestoreId(firestoreId)
                    if (plan == null || !session.isCurrent(account)) return@withTimeout
                    when (action) {
                        ACTION_CONFIRM -> completion(plan)
                        ACTION_CANCEL -> fcmNotificationSender.sendRescheduleToAllDevices(firestoreId)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e(TAG, "Notification action failed (${e.javaClass.simpleName})")
            } finally { pendingResult.finish() }
        }
    }
}

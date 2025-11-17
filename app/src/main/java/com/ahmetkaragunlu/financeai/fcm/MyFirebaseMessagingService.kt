package com.ahmetkaragunlu.financeai.fcm

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var repository: FinanceRepository

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var firestore: FirebaseFirestore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            fcmTokenManager.updateFCMToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (auth.currentUser == null) {
            return
        }
        val data = message.data
        val notificationType = data["type"]
        when (notificationType) {
            "SCHEDULED_REMINDER" -> handleScheduledReminder(data)
            "CANCEL_NOTIFICATION" -> handleCancelNotification(data)
            "DISMISS_NOTIFICATION" -> handleDismissNotification(data)
            "RESCHEDULE_NOTIFICATION" -> handleRescheduleNotification(data)
            else -> Log.w(TAG, "Unknown notification type: $notificationType")
        }
    }

    private fun handleScheduledReminder(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            return
        }
        scope.launch {
            try {
                val activeReminders = firestore.collection("notification_reminders")
                    .whereEqualTo("transactionId", firestoreId)
                    .get()
                    .await()
                if (!activeReminders.isEmpty) {
                    return@launch
                }
            } catch (e: Exception) {
                return@launch
            }

            val localId = try {
                repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
            } catch (e: Exception) {
                null
            }

            if (localId == null) {
                return@launch
            }
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(NotificationWorker.TRANSACTION_ID_KEY to localId)
                )
                .addTag("scheduled_notification_$localId")
                .build()
            WorkManager.getInstance(this@MyFirebaseMessagingService).enqueueUniqueWork(
                "scheduled_notification_$localId",
                androidx.work.ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    private fun handleCancelNotification(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            return
        }
        scope.launch {
            val localId = try {
                repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
            } catch (e: Exception) {
                null
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(firestoreId.hashCode())
            notificationManager.cancel(firestoreId.hashCode() + 20000)

            if (localId != null) {
                WorkManager.getInstance(this@MyFirebaseMessagingService)
                    .cancelAllWorkByTag("scheduled_notification_$localId")
                WorkManager.getInstance(this@MyFirebaseMessagingService)
                    .cancelAllWorkByTag("delete_expired_$localId")
            }
        }
    }

    private fun handleDismissNotification(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            return
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(firestoreId.hashCode())
        notificationManager.cancel(firestoreId.hashCode() + 20000)
    }

    private fun handleRescheduleNotification(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            return
        }
        if (auth.currentUser == null) {
            return
        }
        scope.launch {
            val localId = try {
                repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
            } catch (e: Exception) {
                null
            }
            if (localId != null) {
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(NotificationWorker.TRANSACTION_ID_KEY to localId)
                    )
                    .addTag("scheduled_notification_$localId")
                    .build()
                WorkManager.getInstance(this@MyFirebaseMessagingService).enqueue(workRequest)

            }
        }
    }
}
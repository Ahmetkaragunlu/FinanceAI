package com.ahmetkaragunlu.financeai.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.formatAsCurrency
import com.ahmetkaragunlu.financeai.components.formatAsShortDate
import com.ahmetkaragunlu.financeai.notification.NotificationActionReceiver
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
        Log.d(TAG, "New FCM token: ${token.take(10)}...")

        scope.launch {
            fcmTokenManager.updateFCMToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (auth.currentUser == null) {
            Log.w(TAG, "Message received but user is logged out. Ignoring.")
            return // Oturum açık değilse hiçbir işlem yapma
        }

        Log.d(TAG, "════════════════════════════════════")
        Log.d(TAG, "📩 MESSAGE RECEIVED")
        Log.d(TAG, "From: ${message.from}")
        Log.d(TAG, "Message ID: ${message.messageId}")

        val data = message.data
        Log.d(TAG, "Data keys: ${data.keys.joinToString(", ")}")

        val notificationType = data["type"]
        Log.d(TAG, "Notification type: $notificationType")

        data.forEach { (key, value) ->
            Log.d(TAG, "  $key = $value")
        }
        Log.d(TAG, "════════════════════════════════════")

        when (notificationType) {
            "SCHEDULED_REMINDER" -> handleScheduledReminder(data)
            "CANCEL_NOTIFICATION" -> handleCancelNotification(data)
            "DISMISS_NOTIFICATION" -> handleDismissNotification(data)
            "RESCHEDULE_NOTIFICATION" -> handleRescheduleNotification(data)
            else -> Log.w(TAG, "Unknown notification type: $notificationType")
        }
    }

    private fun handleScheduledReminder(data: Map<String, String>) {
        Log.d(TAG, "🔔 handleScheduledReminder called")

        val firestoreId = data["transactionId"] ?: run {
            Log.e(TAG, "❌ Missing transactionId")
            return
        }

        val amount = data["amount"]?.toDoubleOrNull() ?: run {
            Log.e(TAG, "❌ Invalid amount: ${data["amount"]}")
            return
        }
        val transactionType = data["transactionType"] ?: run {
            Log.e(TAG, "❌ Missing transactionType")
            return
        }
        val category = data["category"] ?: run {
            Log.e(TAG, "❌ Missing category")
            return
        }
        val scheduledDate = data["scheduledDate"]?.toLongOrNull() ?: run {
            Log.e(TAG, "❌ Invalid scheduledDate: ${data["scheduledDate"]}")
            return
        }

        val updateExisting = data["updateExisting"] == "true"

        Log.d(TAG, "📝 Firestore ID: $firestoreId")
        Log.d(TAG, "💰 Amount: $amount")
        Log.d(TAG, "📊 Type: $transactionType")
        Log.d(TAG, "🏷️ Category: $category")
        Log.d(TAG, "🔄 Update Existing: $updateExisting")


        scope.launch {

            try {
                val activeReminders = firestore.collection("notification_reminders")
                    .whereEqualTo("transactionId", firestoreId)
                    .get()
                    .await() // kotlinx.coroutines.tasks.await

                if (!activeReminders.isEmpty) {
                    Log.w(TAG, "🚫 Ignoring SCHEDULED_REMINDER (transactionId: $firestoreId) because an active snooze (notification_reminder) exists.")
                    return@launch // Snooze (erteleme) aktif, bildirimi GÖSTERME
                }
                Log.d(TAG, "✅ No active snooze found for $firestoreId, proceeding with notification.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking for active snooze, aborting notification", e)
                return@launch // Kontrol edilemezse, göstermemek daha güvenli
            }
            //
            val localId = try {
                repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error while fetching local transaction", e)
                null
            }

            if (localId == null) {
                Log.e(TAG, "❌ Local transaction not found for Firestore ID: $firestoreId")
                return@launch // scope.launch'ı sonlandır
            }

            Log.d(TAG, "✅ Found local ID: $localId")

            val formattedAmount = amount.formatAsCurrency()
            val categoryName = category.replace("_", " ").lowercase()
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            val formattedDate = scheduledDate.formatAsShortDate()

            val (title, message) = when (transactionType) {
                "INCOME" -> {
                    getString(R.string.notification_income_title) to
                            getString(
                                R.string.notification_income_message,
                                formattedAmount,
                                categoryName,
                                formattedDate
                            )
                }
                else -> {
                    getString(R.string.notification_expense_title) to
                            getString(
                                R.string.notification_expense_message,
                                formattedAmount,
                                categoryName,
                                formattedDate
                            )
                }
            }

            val confirmIntent = Intent(this@MyFirebaseMessagingService, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CONFIRM
                putExtra(NotificationWorker.FIRESTORE_ID_KEY, firestoreId) // ✅ FirestoreId
            }
            val confirmPendingIntent = PendingIntent.getBroadcast(
                this@MyFirebaseMessagingService,
                firestoreId.hashCode(), // ✅ FirestoreId hash
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cancelIntent = Intent(this@MyFirebaseMessagingService, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CANCEL
                putExtra(NotificationWorker.FIRESTORE_ID_KEY, firestoreId) // ✅ FirestoreId
            }
            val cancelPendingIntent = PendingIntent.getBroadcast(
                this@MyFirebaseMessagingService,
                firestoreId.hashCode() + 10000, // ✅ FirestoreId hash
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val mainIntent = Intent(this@MyFirebaseMessagingService, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                this@MyFirebaseMessagingService,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this@MyFirebaseMessagingService, NotificationWorker.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    getString(R.string.notification_action_yes),
                    confirmPendingIntent
                )
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    getString(R.string.notification_action_no),
                    cancelPendingIntent
                )
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(firestoreId.hashCode(), notification)

            if (updateExisting) {
                Log.d(TAG, "✅ Notification UPDATED successfully (Firestore ID: $firestoreId)")
            } else {
                Log.d(TAG, "✅ Notification shown successfully (Firestore ID: $firestoreId)")
            }
        }
    }
    private fun handleCancelNotification(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            Log.e(TAG, "❌ Missing transactionId for cancel")
            return
        }

        Log.d(TAG, "🗑️ CANCEL_NOTIFICATION received (EVET butonu) - Firestore ID: $firestoreId")

        scope.launch {
            val localId = try {
                repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
            } catch (e: Exception) {
                Log.e(TAG, "Error getting local transaction", e)
                null
            }


            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(firestoreId.hashCode())
            notificationManager.cancel(firestoreId.hashCode() + 20000)

            if (localId != null) {
                Log.d(TAG, "✅ Found local ID: $localId - canceling ALL notifications and work")

                WorkManager.getInstance(this@MyFirebaseMessagingService).cancelAllWorkByTag("scheduled_notification_$localId")
                WorkManager.getInstance(this@MyFirebaseMessagingService).cancelAllWorkByTag("delete_expired_$localId")

                Log.d(TAG, "✅ Notifications and WorkManager cancelled - NO MORE notifications")
            } else {
                Log.w(TAG, "⚠️ Local transaction not found")
            }
        }
    }

    private fun handleDismissNotification(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            Log.e(TAG, "❌ Missing transactionId for dismiss")
            return
        }

        Log.d(TAG, "👋 DISMISS_NOTIFICATION received (HAYIR butonu) - Firestore ID: $firestoreId")


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(firestoreId.hashCode())
        notificationManager.cancel(firestoreId.hashCode() + 20000)

        Log.d(TAG, "✅ Notification dismissed on this device (will return in 15 min on ALL DEVICES)")
    }


    private fun handleRescheduleNotification(data: Map<String, String>) {
        val firestoreId = data["transactionId"] ?: run {
            Log.e(TAG, "❌ Missing transactionId for reschedule")
            return
        }

        Log.d(TAG, "══════════════════════════════════════════════════════════════════")
        Log.d(TAG, "🔄 RESCHEDULE_NOTIFICATION received (15 dakika sonra)")
        Log.d(TAG, "📋 Firestore ID: $firestoreId")

        if (auth.currentUser == null) {
            Log.w(TAG, "🔄 RESCHEDULE_NOTIFICATION received (Firestore ID: $firestoreId) ...")
            Log.w(TAG, "   ... but user is logged out on this device. IGNORING.")

            return
        }


        scope.launch {
            try {
                val activeReminders = firestore.collection("notification_reminders")
                    .whereEqualTo("transactionId", firestoreId)
                    .get()
                    .await() // kotlinx.coroutines.tasks.await

                if (!activeReminders.isEmpty) {
                    Log.w(TAG, "🚫 Ignoring RESCHEDULE (transactionId: $firestoreId) because an active snooze (notification_reminder) exists.")
                    Log.d(TAG, "══════════════════════════════════════════════════════════════════")
                    return@launch // Snooze (erteleme) aktif, yeniden kurma (Worker'ı BAŞLATMA)
                }
                Log.d(TAG, "✅ No active snooze found for $firestoreId, proceeding with reschedule.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking for active snooze, aborting reschedule", e)
                Log.d(TAG, "══════════════════════════════════════════════════════════════════")
                return@launch // Kontrol edilemezse, başlatmamak daha güvenli
            }
            val localId = try {
                repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting local transaction", e)
                null
            }

            if (localId != null) {
                Log.d(TAG, "✅ Found local ID: $localId")
                Log.d(TAG, "🔄 RE-STARTING WorkManager...")

                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(NotificationWorker.TRANSACTION_ID_KEY to localId)
                    )
                    .addTag("scheduled_notification_$localId")
                    .build()

                WorkManager.getInstance(this@MyFirebaseMessagingService).enqueue(workRequest)

                Log.d(TAG, "✅ WorkManager RE-STARTED successfully!")
                Log.d(TAG, "✅ Notifications will continue every 15 min")
                Log.d(TAG, "✅ This happened on ALL DEVICES!")
                Log.d(TAG, "══════════════════════════════════════════════════════════════════")
            } else {
                Log.w(TAG, "⚠️ Local transaction not found for reschedule")
                Log.d(TAG, "══════════════════════════════════════════════════════════════════")
            }
        }
    }

}
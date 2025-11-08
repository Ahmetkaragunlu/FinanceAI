package com.ahmetkaragunlu.financeai.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.formatAsCurrency
import com.ahmetkaragunlu.financeai.components.formatAsShortDate
import com.ahmetkaragunlu.financeai.notification.NotificationActionReceiver
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var repository: FinanceRepository

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
            else -> Log.w(TAG, "Unknown notification type: $notificationType")
        }
    }

    private fun handleScheduledReminder(data: Map<String, String>) {
        Log.d(TAG, "🔔 handleScheduledReminder called")

        // ⚠️ transactionId artık Firestore ID (String)
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

        Log.d(TAG, "📝 Firestore ID: $firestoreId")
        Log.d(TAG, "💰 Amount: $amount")
        Log.d(TAG, "📊 Type: $transactionType")
        Log.d(TAG, "🏷️ Category: $category")

        // Firestore ID ile local ID'yi bul
        val localId = runBlocking {
            repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
        }

        if (localId == null) {
            Log.e(TAG, "❌ Local transaction not found for Firestore ID: $firestoreId")
            return
        }

        Log.d(TAG, "✅ Found local ID: $localId")

        // NotificationWorker ile AYNI STIL
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

        // ⚠️ PendingIntent'lerde LOCAL ID kullan
        val confirmIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CONFIRM
            putExtra(NotificationWorker.TRANSACTION_ID_KEY, localId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            this,
            localId.toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL
            putExtra(NotificationWorker.TRANSACTION_ID_KEY, localId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            localId.toInt() + 10000,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // NotificationWorker ile AYNI NOTIFICATION
        val notification = NotificationCompat.Builder(this, NotificationWorker.CHANNEL_ID)
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
        notificationManager.notify(localId.toInt(), notification)

        Log.d(TAG, "✅ Notification shown successfully (ID: $localId)")
    }

    private fun handleCancelNotification(data: Map<String, String>) {
        // ⚠️ transactionId artık Firestore ID (String)
        val firestoreId = data["transactionId"] ?: run {
            Log.e(TAG, "❌ Missing transactionId for cancel")
            return
        }

        Log.d(TAG, "🗑️ Canceling notification for Firestore ID: $firestoreId")

        // Firestore ID ile local ID'yi bul
        val localId = runBlocking {
            repository.getScheduledTransactionByFirestoreId(firestoreId)?.id
        }

        if (localId == null) {
            Log.w(TAG, "⚠️ Local transaction not found, might be already deleted")
            return
        }

        Log.d(TAG, "✅ Found local ID: $localId, canceling notifications...")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Hem normal hem de expiration bildirimi ID'lerini iptal et
        notificationManager.cancel(localId.toInt())
        notificationManager.cancel(localId.toInt() + 20000)

        Log.d(TAG, "✅ Notifications canceled")
    }
}
package com.ahmetkaragunlu.financeai.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: FinanceRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "scheduled_transaction_channel"
        const val CHANNEL_NAME = "Planlanmış İşlemler"
        const val TRANSACTION_ID_KEY = "transaction_id"
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🚀 Worker started - doWork() called")

            val specificTransactionId = inputData.getLong(TRANSACTION_ID_KEY, -1L)

            if (specificTransactionId != -1L) {
                // Belirli bir transaction için bildirim
                processSpecificTransaction(specificTransactionId)
            } else {
                // Tüm pending transaction'ları kontrol et
                checkAllPendingTransactions()
            }

            Log.d(TAG, "✅ Worker finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Worker failed with error", e)
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun processSpecificTransaction(transactionId: Long) {
        Log.d(TAG, "📍 Processing specific transaction: $transactionId")

        val allTransactions = repository.getAllScheduledTransactions().first()
        val transaction = allTransactions.find { it.id == transactionId }

        transaction?.let {
            Log.d(TAG, "✅ Transaction found: ${it.id}, notificationSent: ${it.notificationSent}")

            if (!it.notificationSent && !it.isConfirmed) {
                sendReminderNotification(it)
                repository.insertScheduledTransaction(
                    it.copy(notificationSent = true)
                )
                Log.d(TAG, "📨 Reminder notification sent for transaction: ${it.id}")
            } else {
                Log.d(TAG, "⚠️ Notification already sent or confirmed: ${it.id}")
            }
        } ?: Log.w(TAG, "❌ Transaction not found: $transactionId")
    }

    private suspend fun checkAllPendingTransactions() {
        Log.d(TAG, "🔍 Checking all pending transactions")

        val currentTime = System.currentTimeMillis()
        val allTransactions = repository.getAllScheduledTransactions().first()

        allTransactions.forEach { transaction ->
            if (transaction.isConfirmed) {
                Log.d(TAG, "✅ Transaction ${transaction.id} already confirmed, skipping")
                return@forEach
            }

            val endOfScheduledDay = getEndOfDay(transaction.scheduledDate)
            val oneDayAfterScheduled = endOfScheduledDay + (24 * 60 * 60 * 1000)

            when {
                // Durum 1: Hatırlatıcı günü henüz gelmedi
                currentTime < transaction.scheduledDate -> {
                    Log.d(TAG, "⏰ Transaction ${transaction.id} is scheduled for future")
                }

                // Durum 2: Hatırlatıcı günü geldi ama gün bitmedi (normal hatırlatıcı gönder)
                currentTime in transaction.scheduledDate..endOfScheduledDay -> {
                    if (!transaction.notificationSent) {
                        Log.d(TAG, "🔔 Sending reminder for transaction: ${transaction.id}")
                        sendReminderNotification(transaction)
                        repository.insertScheduledTransaction(
                            transaction.copy(notificationSent = true)
                        )
                    } else {
                        Log.d(TAG, "⏳ Reminder already sent, waiting for user action: ${transaction.id}")
                    }
                }

                // Durum 3: Hatırlatıcı günü geçti (expiration bildirimi gönder ve sil)
                currentTime > endOfScheduledDay && currentTime < oneDayAfterScheduled -> {
                    if (!transaction.expirationNotificationSent) {
                        Log.d(TAG, "⚠️ Transaction ${transaction.id} EXPIRED! Sending expiration notification")
                        sendExpirationNotification(transaction)
                        repository.insertScheduledTransaction(
                            transaction.copy(expirationNotificationSent = true)
                        )
                    } else {
                        Log.d(TAG, "🗑️ Expiration notification already sent, will delete soon: ${transaction.id}")
                    }
                }

                // Durum 4: Expiration bildiriminden de 1 gün geçti, sil
                currentTime >= oneDayAfterScheduled -> {
                    Log.d(TAG, "🗑️ Deleting expired transaction: ${transaction.id}")
                    repository.deleteScheduledTransaction(transaction)
                }
            }
        }
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun sendReminderNotification(transaction: com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity) {
        Log.d(TAG, "🔔 Preparing REMINDER notification for transaction: ${transaction.id}")

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val formattedAmount = currencyFormat.format(transaction.amount)

        val categoryName = transaction.category.name.replace("_", " ").lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val dateFormat = SimpleDateFormat("d MMMM", Locale("tr", "TR"))
        val formattedDate = dateFormat.format(transaction.scheduledDate)

        val (title, message) = when (transaction.type.name) {
            "INCOME" -> {
                "💰 Gelir Hatırlatıcısı" to
                        "$formattedAmount tutarındaki $categoryName geliriniz ($formattedDate) hesabınıza geçti mi?"
            }
            "EXPENSE" -> {
                "💳 Harcama Hatırlatıcısı" to
                        "$formattedAmount tutarındaki $categoryName ödemenizi ($formattedDate) yaptınız mı?"
            }
            else -> "İşlem Hatırlatıcısı" to "İşleminizi tamamladınız mı?"
        }

        val confirmIntent = Intent(appContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CONFIRM
            putExtra(TRANSACTION_ID_KEY, transaction.id)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            appContext,
            transaction.id.toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(appContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL
            putExtra(TRANSACTION_ID_KEY, transaction.id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            appContext,
            transaction.id.toInt() + 10000,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(appContext, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "EVET", confirmPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "HAYIR", cancelPendingIntent)
            .build()

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(transaction.id.toInt(), notification)

        Log.d(TAG, "✅ Reminder notification displayed for transaction: ${transaction.id}")
    }

    private fun sendExpirationNotification(transaction: com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity) {
        Log.d(TAG, "⚠️ Preparing EXPIRATION notification for transaction: ${transaction.id}")

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val formattedAmount = currencyFormat.format(transaction.amount)

        val categoryName = transaction.category.name.replace("_", " ").lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val dateFormat = SimpleDateFormat("d MMMM", Locale("tr", "TR"))
        val formattedDate = dateFormat.format(transaction.scheduledDate)

        val (title, message) = when (transaction.type.name) {
            "INCOME" -> {
                "⏰ Gelir Hatırlatıcısı Süresi Doldu" to
                        "$formattedAmount tutarındaki $categoryName geliriniz için belirlenen tarih ($formattedDate) geçti. Hatırlatıcı otomatik olarak iptal edildi."
            }
            "EXPENSE" -> {
                "⏰ Harcama Hatırlatıcısı Süresi Doldu" to
                        "$formattedAmount tutarındaki $categoryName ödemeniz için belirlenen tarih ($formattedDate) geçti. Hatırlatıcı otomatik olarak iptal edildi."
            }
            else -> {
                "⏰ Hatırlatıcı Süresi Doldu" to
                        "Belirlenen tarih geçti. Hatırlatıcı otomatik olarak iptal edildi."
            }
        }

        val mainIntent = Intent(appContext, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .build()

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Expiration bildirimi için farklı ID kullan (collision olmasın)
        notificationManager.notify(transaction.id.toInt() + 20000, notification)

        Log.d(TAG, "✅ Expiration notification displayed for transaction: ${transaction.id}")
    }
}

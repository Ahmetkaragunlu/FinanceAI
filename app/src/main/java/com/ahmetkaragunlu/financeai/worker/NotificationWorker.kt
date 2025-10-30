package com.ahmetkaragunlu.financeai.worker

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ahmetkaragunlu.financeai.MainActivity
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.utils.formatAsCurrency
import com.ahmetkaragunlu.financeai.utils.formatAsShortDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: FinanceRepository,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "scheduled_transaction_channel"
        const val TRANSACTION_ID_KEY = "transaction_id"
    }

    override suspend fun doWork(): Result {
        return try {
            val specificTransactionId = inputData.getLong(TRANSACTION_ID_KEY, -1L)

            if (specificTransactionId != -1L) {
                processSpecificTransaction(specificTransactionId)
            } else {
                checkAllPendingTransactions()
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun processSpecificTransaction(transactionId: Long) {
        val allTransactions = repository.getAllScheduledTransactions().first()
        val transaction = allTransactions.find { it.id == transactionId }

        transaction?.let {
            val currentTime = System.currentTimeMillis()
            val endOfScheduledDay = getEndOfDay(it.scheduledDate)

            when {
                currentTime <= endOfScheduledDay -> {
                    sendReminderNotification(it)
                    scheduleNextNotification(transactionId)
                }

                currentTime > endOfScheduledDay -> {
                    if (!it.expirationNotificationSent) {
                        sendExpirationNotification(it)
                        repository.insertScheduledTransaction(
                            it.copy(expirationNotificationSent = true)
                        )
                        scheduleDeleteExpiredTransaction(transactionId)
                    }
                }
            }
        }
    }

    private fun scheduleNextNotification(transactionId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(TRANSACTION_ID_KEY to transactionId)
            )
            .addTag("scheduled_notification_$transactionId")
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                "notification_$transactionId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    private fun scheduleDeleteExpiredTransaction(transactionId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<DeleteExpiredTransactionWorker>()
            .setInitialDelay(24, TimeUnit.HOURS)
            .setInputData(
                workDataOf(TRANSACTION_ID_KEY to transactionId)
            )
            .addTag("delete_expired_$transactionId")
            .build()

        WorkManager.getInstance(appContext).enqueue(workRequest)
    }

    private suspend fun checkAllPendingTransactions() {
        val currentTime = System.currentTimeMillis()
        val allTransactions = repository.getAllScheduledTransactions().first()

        allTransactions.forEach { transaction ->
            val endOfScheduledDay = getEndOfDay(transaction.scheduledDate)

            if (currentTime <= endOfScheduledDay) {
                sendReminderNotification(transaction)
                scheduleNextNotification(transaction.id)
            } else if (!transaction.expirationNotificationSent) {
                sendExpirationNotification(transaction)
                repository.insertScheduledTransaction(
                    transaction.copy(expirationNotificationSent = true)
                )
                scheduleDeleteExpiredTransaction(transaction.id)
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

    private fun sendReminderNotification(transaction: ScheduledTransactionEntity) {
        val formattedAmount = transaction.amount.formatAsCurrency()
        val categoryName = appContext.getString(transaction.category.getDisplayNameRes())
        val formattedDate = transaction.scheduledDate.formatAsShortDate()

        val (title, message) = when (transaction.type) {
            TransactionType.INCOME -> {
                appContext.getString(R.string.notification_income_title) to
                        appContext.getString(
                            R.string.notification_income_message,
                            formattedAmount,
                            categoryName,
                            formattedDate
                        )
            }
            TransactionType.EXPENSE -> {
                appContext.getString(R.string.notification_expense_title) to
                        appContext.getString(
                            R.string.notification_expense_message,
                            formattedAmount,
                            categoryName,
                            formattedDate
                        )
            }
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

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(TRANSACTION_ID_KEY, transaction.id)
            putExtra("FROM_NOTIFICATION", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            appContext,
            transaction.id.toInt() + 30000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(cancelPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                appContext.getString(R.string.notification_action_yes),
                confirmPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                appContext.getString(R.string.notification_action_no),
                cancelPendingIntent
            )
            .build()

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(transaction.id.toInt(), notification)
    }
    private fun sendExpirationNotification(transaction: ScheduledTransactionEntity) {
        val formattedAmount = transaction.amount.formatAsCurrency()
        val categoryName = appContext.getString(transaction.category.getDisplayNameRes())
        val formattedDate = transaction.scheduledDate.formatAsShortDate()

        val (title, message) = when (transaction.type) {
            TransactionType.INCOME -> {
                appContext.getString(R.string.notification_income_expired_title) to
                        appContext.getString(
                            R.string.notification_income_expired_message,
                            formattedAmount,
                            categoryName,
                            formattedDate
                        )
            }
            TransactionType.EXPENSE -> {
                appContext.getString(R.string.notification_expense_expired_title) to
                        appContext.getString(
                            R.string.notification_expense_expired_message,
                            formattedAmount,
                            categoryName,
                            formattedDate
                        )
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
        notificationManager.notify(transaction.id.toInt() + 20000, notification)
    }
}
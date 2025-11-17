package com.ahmetkaragunlu.financeai.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.fcm.FCMNotificationSender
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_CONFIRM = "com.ahmetkaragunlu.financeai.ACTION_CONFIRM"
        const val ACTION_CANCEL = "com.ahmetkaragunlu.financeai.ACTION_CANCEL"
    }

    @Inject
    lateinit var repository: FinanceRepository

    @Inject
    lateinit var firebaseSyncService: FirebaseSyncService

    @Inject
    lateinit var photoStorageManager: PhotoStorageManager

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var fcmNotificationSender: FCMNotificationSender

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val firestoreId = intent.getStringExtra(NotificationWorker.FIRESTORE_ID_KEY)
        if (firestoreId.isNullOrBlank()) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(firestoreId.hashCode())
        notificationManager.cancel(firestoreId.hashCode() + 20000)

        when (intent.action) {
            ACTION_CONFIRM -> {
                handleConfirm(context, firestoreId)
            }
            ACTION_CANCEL -> {
                handleCancel(firestoreId)
            }
        }
    }
    private fun handleConfirm(context: Context, firestoreId: String) {
        scope.launch {
            try {
                val scheduledTransaction = repository.getScheduledTransactionByFirestoreId(firestoreId)
                if (scheduledTransaction != null) {
                    val transaction = TransactionEntity(
                        amount = scheduledTransaction.amount,
                        transaction = scheduledTransaction.type,
                        note = scheduledTransaction.note ?: "",
                        date = System.currentTimeMillis(),
                        category = scheduledTransaction.category,
                        photoUri = scheduledTransaction.photoUri,
                        locationFull = scheduledTransaction.locationFull,
                        locationShort = scheduledTransaction.locationShort,
                        latitude = scheduledTransaction.latitude,
                        longitude = scheduledTransaction.longitude,
                        syncedToFirebase = false
                    )
                    val transactionSyncResult = firebaseSyncService.syncTransactionToFirebase(transaction)
                    if (transactionSyncResult.isSuccess) {
                        val transactionFirestoreId = transactionSyncResult.getOrNull()!!
                        val transactionWithId = transaction.copy(
                            firestoreId = transactionFirestoreId,
                            syncedToFirebase = true
                        )
                        repository.insertTransaction(transactionWithId)

                        if (!scheduledTransaction.photoUri.isNullOrBlank() && scheduledTransaction.firestoreId.isNotEmpty()) {
                            photoStorageManager.moveScheduledPhotoToTransaction(
                                scheduledFirestoreId = scheduledTransaction.firestoreId,
                                transactionFirestoreId = transactionFirestoreId
                            )
                        }
                        if (scheduledTransaction.firestoreId.isNotEmpty()) {
                            val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                                scheduledTransaction.firestoreId
                            )
                        }
                        repository.deleteScheduledTransaction(scheduledTransaction)
                        WorkManager.getInstance(context).cancelAllWorkByTag("scheduled_notification_${scheduledTransaction.id}")
                        WorkManager.getInstance(context).cancelAllWorkByTag("delete_expired_${scheduledTransaction.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, " Error in CONFIRM action", e)
            }
        }
    }
    private fun handleCancel(firestoreId: String) {
        scope.launch {
            try {
                val scheduledTransaction = repository.getScheduledTransactionByFirestoreId(firestoreId)
                if (scheduledTransaction != null) {
                    fcmNotificationSender.sendRescheduleToAllDevices(firestoreId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in CANCEL action", e)
            }
        }
    }
}
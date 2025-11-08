package com.ahmetkaragunlu.financeai.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getLongExtra(NotificationWorker.TRANSACTION_ID_KEY, -1L)
        if (transactionId == -1L) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(transactionId.toInt())
        notificationManager.cancel(transactionId.toInt() + 20000)

        when (intent.action) {
            ACTION_CONFIRM -> {
                scope.launch {
                    try {
                        Log.d(TAG, "CONFIRM action - Local ID: $transactionId")

                        val scheduledTransaction = repository.getScheduledTransactionById(transactionId)

                        if (scheduledTransaction != null) {
                            Log.d(TAG, "Found scheduled transaction - Firestore ID: ${scheduledTransaction.firestoreId}")

                            // 1. Transaction oluştur (lokal path ile)
                            val transaction = TransactionEntity(
                                amount = scheduledTransaction.amount,
                                transaction = scheduledTransaction.type,
                                note = scheduledTransaction.note ?: "",
                                date = System.currentTimeMillis(),
                                category = scheduledTransaction.category,
                                photoUri = scheduledTransaction.photoUri, // Lokal path
                                locationFull = scheduledTransaction.locationFull,
                                locationShort = scheduledTransaction.locationShort,
                                latitude = scheduledTransaction.latitude,
                                longitude = scheduledTransaction.longitude,
                                syncedToFirebase = false
                            )

                            // 2. Local'e kaydet (önce local, sonra sync)
                            repository.insertTransaction(transaction)
                            Log.d(TAG, "Transaction inserted to local DB")

                            // 3. Firebase'e sync et
                            val transactionSyncResult = firebaseSyncService.syncTransactionToFirebase(transaction)

                            if (transactionSyncResult.isSuccess) {
                                val transactionFirestoreId = transactionSyncResult.getOrNull()!!
                                Log.d(TAG, "Transaction synced to Firebase: $transactionFirestoreId")

                                // 4. Fotoğraf varsa Storage'da taşı (scheduled/ → transactions/)
                                if (!scheduledTransaction.photoUri.isNullOrBlank() && scheduledTransaction.firestoreId.isNotEmpty()) {
                                    val moveResult = photoStorageManager.moveScheduledPhotoToTransaction(
                                        scheduledFirestoreId = scheduledTransaction.firestoreId,
                                        transactionFirestoreId = transactionFirestoreId
                                    )

                                    if (moveResult.isSuccess) {
                                        Log.d(TAG, "Photo moved in Storage from scheduled to transaction")
                                    } else {
                                        Log.e(TAG, "Failed to move photo in Storage", moveResult.exceptionOrNull())
                                    }
                                }

                                // 5. Scheduled transaction'ı Firebase'den sil
                                if (scheduledTransaction.firestoreId.isNotEmpty()) {
                                    val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                                        scheduledTransaction.firestoreId
                                    )

                                    if (deleteResult.isSuccess) {
                                        Log.d(TAG, "Scheduled deleted from Firebase - all devices notified")
                                    } else {
                                        Log.e(TAG, "Failed to delete scheduled from Firebase", deleteResult.exceptionOrNull())
                                    }
                                }
                            } else {
                                Log.e(TAG, "Failed to sync transaction to Firebase", transactionSyncResult.exceptionOrNull())
                            }

                            // 6. Local'den scheduled'ı sil
                            repository.deleteScheduledTransaction(scheduledTransaction)
                            Log.d(TAG, "Scheduled transaction deleted from local DB")

                            // 7. Bildirimleri iptal et
                            cancelAllPendingNotifications(context, transactionId)
                            Log.d(TAG, "All pending notifications canceled")

                        } else {
                            Log.e(TAG, "Scheduled transaction not found with ID: $transactionId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in CONFIRM action", e)
                    }
                }
            }

            ACTION_CANCEL -> {
                scope.launch {
                    try {
                        Log.d(TAG, "CANCEL action - Local ID: $transactionId")

                        val scheduledTransaction = repository.getScheduledTransactionById(transactionId)

                        if (scheduledTransaction != null && scheduledTransaction.firestoreId.isNotEmpty()) {
                            Log.d(TAG, "Deleting scheduled transaction - Firestore ID: ${scheduledTransaction.firestoreId}")

                            // Firebase'den sil (Storage'daki fotoğraf da silinecek - FirebaseSyncService'te)
                            val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                                scheduledTransaction.firestoreId
                            )

                            if (deleteResult.isSuccess) {
                                Log.d(TAG, "Scheduled deleted from Firebase - all devices notified")
                            } else {
                                Log.e(TAG, "Failed to delete from Firebase", deleteResult.exceptionOrNull())
                            }

                            // Local'den sil
                            repository.deleteScheduledTransaction(scheduledTransaction)
                            Log.d(TAG, "Scheduled transaction deleted from local DB")

                            // Bildirimleri iptal et
                            cancelAllPendingNotifications(context, transactionId)
                            Log.d(TAG, "All pending notifications canceled")

                        } else {
                            Log.w(TAG, "Scheduled transaction not found or no Firestore ID")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in CANCEL action", e)
                    }
                }
            }
        }
    }

    private fun cancelAllPendingNotifications(context: Context, transactionId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("scheduled_notification_$transactionId")
        WorkManager.getInstance(context).cancelAllWorkByTag("delete_expired_$transactionId")
    }
}
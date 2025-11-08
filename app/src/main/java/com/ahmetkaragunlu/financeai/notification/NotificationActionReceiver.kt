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

        // Önce bu cihazdan bildirimi kaldır
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(transactionId.toInt())
        notificationManager.cancel(transactionId.toInt() + 20000)

        when (intent.action) {
            ACTION_CONFIRM -> {
                scope.launch {
                    try {
                        Log.d(TAG, "✅ CONFIRM action - Local ID: $transactionId")

                        val scheduledTransaction = repository.getScheduledTransactionById(transactionId)

                        if (scheduledTransaction != null) {
                            Log.d(TAG, "Found scheduled - Firestore ID: ${scheduledTransaction.firestoreId}")

                            // 1. Transaction oluştur
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

                            // 2. Local'e kaydet
                            repository.insertTransaction(transaction)
                            Log.d(TAG, "Transaction inserted to local")

                            // 3. Firebase'e sync et
                            val transactionSyncResult = firebaseSyncService.syncTransactionToFirebase(transaction)

                            if (transactionSyncResult.isSuccess) {
                                val transactionFirestoreId = transactionSyncResult.getOrNull()!!
                                Log.d(TAG, "Transaction synced: $transactionFirestoreId")

                                // 4. Fotoğraf varsa taşı
                                if (!scheduledTransaction.photoUri.isNullOrBlank() && scheduledTransaction.firestoreId.isNotEmpty()) {
                                    val moveResult = photoStorageManager.moveScheduledPhotoToTransaction(
                                        scheduledFirestoreId = scheduledTransaction.firestoreId,
                                        transactionFirestoreId = transactionFirestoreId
                                    )

                                    if (moveResult.isSuccess) {
                                        Log.d(TAG, "Photo moved successfully")
                                    } else {
                                        Log.e(TAG, "Photo move failed", moveResult.exceptionOrNull())
                                    }
                                }

                                // 5. ⚠️ ÖNEMLİ: Scheduled'ı Firebase'den sil
                                // Bu silme işlemi Functions'taki onDelete trigger'ı tetikleyecek
                                // ve TÜM CİHAZLARA "CANCEL_NOTIFICATION" mesajı gönderecek!
                                if (scheduledTransaction.firestoreId.isNotEmpty()) {
                                    val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                                        scheduledTransaction.firestoreId
                                    )

                                    if (deleteResult.isSuccess) {
                                        Log.d(TAG, "✅ Scheduled deleted from Firebase - all devices will be notified")
                                    } else {
                                        Log.e(TAG, "❌ Delete failed", deleteResult.exceptionOrNull())
                                    }
                                }
                            } else {
                                Log.e(TAG, "Transaction sync failed", transactionSyncResult.exceptionOrNull())
                            }

                            // 6. Local'den sil
                            repository.deleteScheduledTransaction(scheduledTransaction)
                            Log.d(TAG, "Deleted from local DB")

                            // 7. Bu cihazın pending notification'larını iptal et
                            cancelAllPendingNotifications(context, transactionId)

                        } else {
                            Log.e(TAG, "Scheduled transaction not found: $transactionId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in CONFIRM action", e)
                    }
                }
            }

            ACTION_CANCEL -> {
                scope.launch {
                    try {
                        Log.d(TAG, "❌ CANCEL action - Local ID: $transactionId")

                        val scheduledTransaction = repository.getScheduledTransactionById(transactionId)

                        if (scheduledTransaction != null && scheduledTransaction.firestoreId.isNotEmpty()) {
                            Log.d(TAG, "Deleting scheduled - Firestore ID: ${scheduledTransaction.firestoreId}")

                            // ⚠️ ÖNEMLİ: Firebase'den sil
                            // Bu Functions'taki onDelete trigger'ı tetikleyecek
                            // ve TÜM CİHAZLARA "CANCEL_NOTIFICATION" mesajı gönderecek!
                            val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                                scheduledTransaction.firestoreId
                            )

                            if (deleteResult.isSuccess) {
                                Log.d(TAG, "✅ Scheduled deleted from Firebase - all devices will be notified")
                            } else {
                                Log.e(TAG, "❌ Delete failed", deleteResult.exceptionOrNull())
                            }

                            // Local'den sil
                            repository.deleteScheduledTransaction(scheduledTransaction)
                            Log.d(TAG, "Deleted from local DB")

                            // Bu cihazın pending notification'larını iptal et
                            cancelAllPendingNotifications(context, transactionId)

                        } else {
                            Log.w(TAG, "Scheduled not found or no Firestore ID")
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
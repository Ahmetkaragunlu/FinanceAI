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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        // 🔥 ÖNEMLİ DEĞİŞİKLİK: FirestoreId kullan
        val firestoreId = intent.getStringExtra(NotificationWorker.FIRESTORE_ID_KEY)
        if (firestoreId.isNullOrBlank()) return

        // ⚡ HIZLI ÇÖZÜM: Bildirimi HEMEN kapat
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(firestoreId.hashCode())
        notificationManager.cancel(firestoreId.hashCode() + 20000)

        when (intent.action) {
            ACTION_CONFIRM -> {
                scope.launch {
                    try {
                        Log.d(TAG, "✅ CONFIRM action (EVET butonu) - Firestore ID: $firestoreId")

                        val scheduledTransaction = repository.getScheduledTransactionByFirestoreId(firestoreId)

                        if (scheduledTransaction != null) {
                            Log.d(TAG, "📋 Found scheduled transaction")

                            // Normal transaction olarak kaydet
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

                            // 1️⃣ Firebase'e sync et
                            val transactionSyncResult = firebaseSyncService.syncTransactionToFirebase(transaction)

                            if (transactionSyncResult.isSuccess) {
                                val transactionFirestoreId = transactionSyncResult.getOrNull()!!
                                Log.d(TAG, "✅ Transaction synced: $transactionFirestoreId")

                                // 2️⃣ Room'a kaydet
                                val transactionWithId = transaction.copy(
                                    firestoreId = transactionFirestoreId,
                                    syncedToFirebase = true
                                )
                                repository.insertTransaction(transactionWithId)
                                Log.d(TAG, "✅ Transaction inserted to Room")

                                // 3️⃣ Fotoğrafı taşı
                                if (!scheduledTransaction.photoUri.isNullOrBlank() && scheduledTransaction.firestoreId.isNotEmpty()) {
                                    photoStorageManager.moveScheduledPhotoToTransaction(
                                        scheduledFirestoreId = scheduledTransaction.firestoreId,
                                        transactionFirestoreId = transactionFirestoreId
                                    )
                                }

                                // 4️⃣ Scheduled Transaction'ı Firebase'den sil
                                // Bu silme TÜM CİHAZLARA CANCEL_NOTIFICATION gönderecek!
                                if (scheduledTransaction.firestoreId.isNotEmpty()) {
                                    val deleteResult = firebaseSyncService.deleteScheduledTransactionFromFirebase(
                                        scheduledTransaction.firestoreId
                                    )

                                    if (deleteResult.isSuccess) {
                                        Log.d(TAG, "✅ Scheduled deleted from Firebase")
                                        Log.d(TAG, "✅ CANCEL_NOTIFICATION sent to ALL DEVICES")
                                    }
                                }

                                // 5️⃣ Local'den sil
                                repository.deleteScheduledTransaction(scheduledTransaction)
                                Log.d(TAG, "✅ Deleted from local DB")

                                // 6️⃣ Bu cihazın WorkManager'ını iptal et
                                WorkManager.getInstance(context).cancelAllWorkByTag("scheduled_notification_${scheduledTransaction.id}")
                                WorkManager.getInstance(context).cancelAllWorkByTag("delete_expired_${scheduledTransaction.id}")

                            } else {
                                Log.e(TAG, "❌ Transaction sync failed", transactionSyncResult.exceptionOrNull())
                            }

                        } else {
                            Log.e(TAG, "❌ Scheduled transaction not found: $firestoreId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in CONFIRM action", e)
                    }
                }
            }

            ACTION_CANCEL -> {
                scope.launch {
                    try {
                        Log.d(TAG, "❌ CANCEL action (HAYIR butonu) - Firestore ID: $firestoreId")
                        Log.d(TAG, "══════════════════════════════════════════════════════════════════")

                        val scheduledTransaction = repository.getScheduledTransactionByFirestoreId(firestoreId)

                        if (scheduledTransaction != null) {
                            Log.d(TAG, "📋 User clicked NO")

                            // ⚡ PARALEL: Dismiss ve Reminder'ı aynı anda gönder
                            val dismissJob = scope.launch {
                                try {
                                    val dismissData = hashMapOf(
                                        "transactionId" to firestoreId,
                                        "timestamp" to System.currentTimeMillis(),
                                        "dismissedBy" to "user_action"
                                    )

                                    val dismissDocRef = firestore.collection("notification_dismissals")
                                        .add(dismissData)
                                        .await()

                                    Log.d(TAG, "✅ STEP 1/2: Dismiss signal sent to ALL DEVICES")
                                    Log.d(TAG, "   Document ID: ${dismissDocRef.id}")

                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ STEP 1/2 FAILED: Dismiss signal", e)
                                }
                            }

                            val reminderJob = scope.launch {
                                try {
                                    val reminderData = hashMapOf(
                                        "transactionId" to firestoreId,
                                        "timestamp" to System.currentTimeMillis(),
                                        "triggerIn15Minutes" to true
                                    )

                                    val reminderDocRef = firestore.collection("notification_reminders")
                                        .add(reminderData)
                                        .await()

                                    Log.d(TAG, "✅ STEP 2/2: Reminder scheduled for ALL DEVICES")
                                    Log.d(TAG, "   Document ID: ${reminderDocRef.id}")
                                    Log.d(TAG, "   ⏰ Firebase Function will trigger in 15 minutes")

                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ STEP 2/2 FAILED: Schedule reminder", e)
                                }
                            }

                            // Her ikisinin de bitmesini bekle
                            dismissJob.join()
                            reminderJob.join()

                            Log.d(TAG, "")
                            Log.d(TAG, "📱 WHAT HAPPENS NEXT:")
                            Log.d(TAG, "   1. ALL DEVICES dismiss notification (DISMISS_NOTIFICATION)")
                            Log.d(TAG, "   2. Firebase Function waits 15 minutes")
                            Log.d(TAG, "   3. Firebase sends RESCHEDULE_NOTIFICATION to ALL DEVICES")
                            Log.d(TAG, "   4. ALL DEVICES restart WorkManager")
                            Log.d(TAG, "══════════════════════════════════════════════════════════════════")

                        } else {
                            Log.w(TAG, "⚠️ Scheduled not found")
                            Log.d(TAG, "══════════════════════════════════════════════════════════════════")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in CANCEL action", e)
                        Log.d(TAG, "══════════════════════════════════════════════════════════════════")
                    }
                }
            }
        }
    }
}
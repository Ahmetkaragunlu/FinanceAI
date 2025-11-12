package com.ahmetkaragunlu.financeai.fcm


import android.util.Log
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMNotificationSender @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val fcmTokenManager: FCMTokenManager
) {
    companion object {
        private const val TAG = "FCMNotificationSender"
    }

    suspend fun sendScheduledReminderToAllDevices(transaction: ScheduledTransactionEntity) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val tokens = fcmTokenManager.getUserFCMTokens(userId)

            Log.d(TAG, "═══════════════════════════════════════════════════")
            Log.d(TAG, "📡 Sending notification to ${tokens.size} devices")
            Log.d(TAG, "Firestore ID: ${transaction.firestoreId}")

            if (tokens.isEmpty()) {
                Log.w(TAG, "⚠️ No FCM tokens found for user")
                return
            }

            // Firebase Functions'a trigger gönder
            val data = hashMapOf(
                "transactionId" to transaction.firestoreId,
                "type" to "MANUAL_SEND_TO_ALL",
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("notification_triggers")
                .add(data)
                .await()

            Log.d(TAG, "✅ Trigger sent to Firebase Functions")
            Log.d(TAG, "═══════════════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification trigger", e)
        }
    }

    suspend fun sendRescheduleToAllDevices(firestoreId: String) {
        try {
            val data = hashMapOf(
                "transactionId" to firestoreId,
                "triggerIn15Minutes" to true,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("notification_reminders")
                .add(data)
                .await()

            Log.d(TAG, "✅ Reschedule trigger sent for: $firestoreId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending reschedule trigger", e)
        }
    }

    suspend fun sendDismissToAllDevices(firestoreId: String) {
        try {
            val data = hashMapOf(
                "transactionId" to firestoreId,
                "timestamp" to System.currentTimeMillis(),
                "dismissedBy" to "user_action"
            )

            firestore.collection("notification_dismissals")
                .add(data)
                .await()

            Log.d(TAG, "✅ Dismiss signal sent for: $firestoreId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending dismiss signal", e)
        }
    }
}
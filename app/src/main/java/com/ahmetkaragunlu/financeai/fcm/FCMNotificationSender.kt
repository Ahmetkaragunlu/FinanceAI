package com.ahmetkaragunlu.financeai.fcm


import android.util.Log
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
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

    suspend fun sendRescheduleToAllDevices(firestoreId: String) {
        try {
            val userId = auth.currentUser?.uid ?: run {
                Log.e(TAG, "❌ Cannot create reminder, user not logged in")
                return
            }

            Log.d(TAG, "🗑️ Deleting old reminders for: $firestoreId")
            val batch = firestore.batch()
            val oldReminders = firestore.collection("notification_reminders")
                .whereEqualTo("transactionId", firestoreId)
                .get()
                .await()

            if (!oldReminders.isEmpty) {
                oldReminders.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                Log.d(TAG, "🗑️ Deleted ${oldReminders.size()} old reminders")
            }

            val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)
            val data = hashMapOf(
                "transactionId" to firestoreId,
                "userId" to userId,
                "triggerTime" to triggerTime,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("notification_reminders")
                .add(data)
                .await()

            Log.d(TAG, "✅ New reschedule trigger (reminder) saved to Firebase for: $firestoreId")
            Log.d(TAG, "   Trigger will fire at: ${java.util.Date(triggerTime)}")

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
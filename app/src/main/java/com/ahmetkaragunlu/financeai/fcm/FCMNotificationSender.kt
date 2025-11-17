
package com.ahmetkaragunlu.financeai.fcm

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
) {
    suspend fun sendRescheduleToAllDevices(firestoreId: String) {
        try {
            val userId = auth.currentUser?.uid ?: run {
                return
            }
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
        } catch (e: Exception) {
        }
    }
}

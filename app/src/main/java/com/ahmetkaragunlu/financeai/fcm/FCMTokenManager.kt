
package com.ahmetkaragunlu.financeai.fcm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FCM_TOKENS_FIELD = "fcmTokens"
    }

    suspend fun updateFCMToken() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val token = messaging.token.await()
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(FCM_TOKENS_FIELD, FieldValue.arrayUnion(token))
                .await()
        } catch (e: Exception) {
        }
    }
    suspend fun removeFCMToken() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val token = messaging.token.await()
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(FCM_TOKENS_FIELD, FieldValue.arrayRemove(token))
                .await()
        } catch (e: Exception) {
        }
    }
}

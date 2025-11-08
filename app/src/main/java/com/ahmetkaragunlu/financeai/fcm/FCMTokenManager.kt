package com.ahmetkaragunlu.financeai.fcm

import android.util.Log
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
        private const val TAG = "FCMTokenManager"
        private const val USERS_COLLECTION = "users"
        private const val FCM_TOKENS_FIELD = "fcmTokens"
    }

    suspend fun updateFCMToken() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val token = messaging.token.await()

            Log.d(TAG, "FCM Token obtained: ${token.take(10)}...")

            // Token'ı array'e ekle (duplicate olmasın diye arrayUnion kullan)
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(FCM_TOKENS_FIELD, FieldValue.arrayUnion(token))
                .await()

            Log.d(TAG, "FCM Token saved to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
        }
    }

    suspend fun removeFCMToken() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val token = messaging.token.await()

            // Token'ı array'den kaldır
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(FCM_TOKENS_FIELD, FieldValue.arrayRemove(token))
                .await()

            Log.d(TAG, "FCM Token removed from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing FCM token", e)
        }
    }

    suspend fun getUserFCMTokens(userId: String): List<String> {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            doc.get(FCM_TOKENS_FIELD) as? List<String> ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user FCM tokens", e)
            emptyList()
        }
    }
}
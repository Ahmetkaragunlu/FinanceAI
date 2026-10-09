package com.ahmetkaragunlu.financeai.fcm

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.firebase.UserFields
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.work.AccountWork
import com.ahmetkaragunlu.financeai.fcm.data.local.entity.TokenOperation
import com.ahmetkaragunlu.financeai.fcm.work.TokenRegistrationWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FCMTokenManager @Inject constructor(
    private val firestore: FirebaseFirestore, private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging, private val database: FinanceDatabase,
    private val workManager: WorkManager, private val clock: Clock,
    private val functions: FirebaseFunctions
) {
    suspend fun updateFCMToken() {
        val owner = auth.currentUser?.uid ?: return
        val token = messaging.token.await()
        if (auth.currentUser?.uid == owner) queue(owner, token, false)
    }
    suspend fun suppliedToken(token: String) {
        val owner = auth.currentUser?.uid ?: return
        queue(owner, token, false)
    }
    suspend fun removeFCMToken() {
        val owner = auth.currentUser?.uid ?: return
        val token = database.tokenOperationDao().latestRegistered(owner)?.token ?: messaging.token.await()
        if (auth.currentUser?.uid != owner) return
        queue(owner, token, true)
        // Rotation also prevents a former account from continuing to target this installation.
        messaging.deleteToken().await()
        // Revoke while credentials remain available; an outage keeps the durable intention.
        flush(owner)
    }
    fun restore(owner: String) {
        val work = OneTimeWorkRequestBuilder<TokenRegistrationWorker>()
            .setInputData(workDataOf(SyncScheduler.OWNER_ID to owner, TokenRegistrationWorker.FETCH_CURRENT to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(AccountWork.tag(owner)).build()
        workManager.enqueueUniqueWork("token_restore_$owner", ExistingWorkPolicy.KEEP, work)
    }
    suspend fun restorePlans(owner: String) {
        if (auth.currentUser?.uid != owner || auth.currentUser?.isEmailVerified != true) return
        val token = database.tokenOperationDao().latestRegistered(owner)?.token ?: return
        var cursor: String? = null
        do {
            if (auth.currentUser?.uid != owner) return
            val result = functions.getHttpsCallable("restoreScheduleState")
                .call(mapOf("deviceToken" to token, "cursor" to cursor)).await().data as? Map<*, *>
                ?: throw DataAccessException.InvalidRemoteData()
            val next = result["nextCursor"] as? String
            if (next != null && next == cursor) throw DataAccessException.InvalidRemoteData()
            cursor = next
        } while (cursor != null)
    }
    private suspend fun queue(owner: String, token: String, remove: Boolean) {
        require(token.isNotBlank())
        database.tokenOperationDao().save(TokenOperation(owner, token, remove, clock.millis()))
        val work = OneTimeWorkRequestBuilder<TokenRegistrationWorker>()
            .setInputData(workDataOf(SyncScheduler.OWNER_ID to owner))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(AccountWork.tag(owner)).build()
        workManager.enqueueUniqueWork("token_registration_$owner", ExistingWorkPolicy.APPEND_OR_REPLACE, work)
    }
    suspend fun flush(owner: String) {
        for (operation in database.tokenOperationDao().pending(owner)) {
            if (auth.currentUser?.uid != owner) return
            firestore.collection(FirestoreCollections.USERS).document(owner).update(UserFields.FCM_TOKENS,
                if (operation.remove) FieldValue.arrayRemove(operation.token) else FieldValue.arrayUnion(operation.token)).await()
            if (auth.currentUser?.uid != owner) return
            database.tokenOperationDao().acknowledge(owner, operation.token, operation.createdAt, operation.remove)
        }
    }
}

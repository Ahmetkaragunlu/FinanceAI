package com.ahmetkaragunlu.financeai.firebasesync

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.functions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val localRepository: FinanceRepository,
    private val photoStorageManager: PhotoStorageManager,
    private val messaging: FirebaseMessaging,
    @ApplicationContext private val context: Context
) {
    private var transactionListener: ListenerRegistration? = null
    private var scheduledListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    private val recentlyAddedIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val SCHEDULED_COLLECTION = "scheduled_transactions"
        private const val RECENTLY_ADDED_TIMEOUT = 3000L
    }

    // --- ID GENERATION ---
    fun getNewTransactionId(): String = firestore.collection(TRANSACTIONS_COLLECTION).document().id
    fun getNewScheduledTransactionId(): String = firestore.collection(SCHEDULED_COLLECTION).document().id
    private fun getUserId(): String? = auth.currentUser?.uid

    // --- INITIALIZATION ---
    fun initializeSyncAfterLogin() {
        scope.launch {
            if (getUserId() == null || isInitialized) return@launch
            try {
                performInitialSync()
                pushUnsyncedData()
                delay(300)
                startListeningToTransactions()
                startListeningToScheduledTransactions()
                delay(500)
                sendPendingNotifications()
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync initialization", e)
            }
        }
    }

    fun resetSync() {
        stopListening()
        recentlyAddedIds.clear()
        isInitialized = false
    }

    private fun markAsRecentlyAdded(firestoreId: String) {
        recentlyAddedIds.add(firestoreId)
        scope.launch {
            delay(RECENTLY_ADDED_TIMEOUT)
            recentlyAddedIds.remove(firestoreId)
        }
    }

    // --- PUSH DATA (LOCAL -> FIREBASE) ---
    private suspend fun pushUnsyncedData() {
        try {
            localRepository.getUnsyncedTransactions().firstOrNull()?.forEach { transaction ->
                syncTransactionToFirebase(transaction).onSuccess {
                    localRepository.updateTransaction(transaction.copy(syncedToFirebase = true))
                }
            }
            localRepository.getUnsyncedScheduledTransactions().firstOrNull()?.forEach { transaction ->
                syncScheduledTransactionToFirebase(transaction).onSuccess {
                    localRepository.updateScheduledTransaction(transaction.copy(syncedToFirebase = true))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing unsynced data", e)
        }
    }

    suspend fun syncTransactionToFirebase(transaction: TransactionEntity): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            val docRef = if (transaction.firestoreId.isNotEmpty()) {
                firestore.collection(TRANSACTIONS_COLLECTION).document(transaction.firestoreId)
            } else {
                firestore.collection(TRANSACTIONS_COLLECTION).document()
            }
            val firestoreId = docRef.id
            markAsRecentlyAdded(firestoreId)
            val data = hashMapOf(
                "userId" to userId,
                "amount" to transaction.amount,
                "transaction" to transaction.transaction.name,
                "note" to transaction.note,
                "date" to transaction.date,
                "category" to transaction.category.name,
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncScheduledTransactionToFirebase(transaction: ScheduledTransactionEntity): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            val docRef = if (transaction.firestoreId.isNotEmpty()) {
                firestore.collection(SCHEDULED_COLLECTION).document(transaction.firestoreId)
            } else {
                firestore.collection(SCHEDULED_COLLECTION).document()
            }
            val firestoreId = docRef.id
            markAsRecentlyAdded(firestoreId)

            val data = hashMapOf(
                "userId" to userId,
                "amount" to transaction.amount,
                "type" to transaction.type.name,
                "category" to transaction.category.name,
                "note" to transaction.note,
                "scheduledDate" to transaction.scheduledDate,
                "expirationNotificationSent" to transaction.expirationNotificationSent,
                "notificationSent" to transaction.notificationSent,
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DELETE OPERATIONS ---
    suspend fun deleteTransactionPhoto(firestoreId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection(TRANSACTIONS_COLLECTION).document(firestoreId)
            val doc = docRef.get().await()
            val photoUrl = doc.getString("photoStorageUrl")

            if (!photoUrl.isNullOrBlank()) {
                scope.launch {
                    try {
                        photoStorageManager.deletePhoto(photoUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting photo blob", e)
                    }
                }
            }
            docRef.update("photoStorageUrl", null).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionFromFirebase(firestoreId: String): Result<Unit> {
        return deleteDocAndPhoto(TRANSACTIONS_COLLECTION, firestoreId)
    }

    suspend fun deleteScheduledTransactionFromFirebase(firestoreId: String): Result<Unit> {
        return deleteDocAndPhoto(SCHEDULED_COLLECTION, firestoreId)
    }

    private suspend fun deleteDocAndPhoto(collection: String, firestoreId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(collection).document(firestoreId).get().await()
            val photoUrl = doc.getString("photoStorageUrl")
            if (!photoUrl.isNullOrBlank()) {
                scope.launch {
                    try {
                        photoStorageManager.deletePhoto(photoUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting photo", e)
                    }
                }
            }
            firestore.collection(collection).document(firestoreId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- INITIAL FETCH (PULL) ---
    private suspend fun performInitialSync() {
        val userId = getUserId() ?: return
        try {
            val tJob = scope.async { fetchAllFromFirebase(TRANSACTIONS_COLLECTION, userId, isScheduled = false) }
            val sJob = scope.async { fetchAllFromFirebase(SCHEDULED_COLLECTION, userId, isScheduled = true) }
            tJob.await()
            sJob.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync", e)
        }
    }

    private suspend fun fetchAllFromFirebase(collection: String, userId: String, isScheduled: Boolean) {
        try {
            val snapshot = firestore.collection(collection)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id
                val exists = if (isScheduled) {
                    localRepository.getScheduledTransactionByFirestoreId(firestoreId) != null
                } else {
                    localRepository.getTransactionByFirestoreId(firestoreId) != null
                }

                if (!exists) {
                    handlePhotoDownloadAndInsert(doc, firestoreId, isScheduled)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Firebase", e)
        }
    }

    // --- LISTENERS (REAL-TIME) ---
    private fun startListeningToTransactions() {
        val userId = getUserId() ?: return
        transactionListener?.remove()
        transactionListener = createListener(TRANSACTIONS_COLLECTION, userId, isScheduled = false)
    }

    private fun startListeningToScheduledTransactions() {
        val userId = getUserId() ?: return
        scheduledListener?.remove()
        scheduledListener = createListener(SCHEDULED_COLLECTION, userId, isScheduled = true)
    }

    private fun createListener(collection: String, userId: String, isScheduled: Boolean): ListenerRegistration {
        return firestore.collection(collection)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                processDocumentChange(change, isScheduled)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing change", e)
                            }
                        }
                    }
                }
            }
    }

    private suspend fun processDocumentChange(change: DocumentChange, isScheduled: Boolean) {
        val doc = change.document
        val firestoreId = doc.id

        when (change.type) {
            DocumentChange.Type.ADDED -> {
                if (recentlyAddedIds.contains(firestoreId)) return

                val exists = if (isScheduled) {
                    localRepository.getScheduledTransactionByFirestoreId(firestoreId) != null
                } else {
                    localRepository.getTransactionByFirestoreId(firestoreId) != null
                }
                if (exists) return

                handlePhotoDownloadAndInsert(doc, firestoreId, isScheduled)
            }

            DocumentChange.Type.MODIFIED -> {
                if (isScheduled) {
                    val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId) ?: return
                    val localPhotoPath = handlePhotoUpdate(doc, existing.photoUri, firestoreId, isScheduled)

                    val updated = existing.copy(
                        amount = doc.getDouble("amount") ?: existing.amount,
                        type = TransactionType.valueOf(doc.getString("type") ?: existing.type.name),
                        category = CategoryType.valueOf(doc.getString("category") ?: existing.category.name),
                        note = doc.getString("note") ?: existing.note,
                        scheduledDate = doc.getLong("scheduledDate") ?: existing.scheduledDate,
                        expirationNotificationSent = doc.getBoolean("expirationNotificationSent") ?: existing.expirationNotificationSent,
                        notificationSent = doc.getBoolean("notificationSent") ?: existing.notificationSent,
                        photoUri = localPhotoPath,
                        locationFull = doc.getString("locationFull") ?: existing.locationFull,
                        locationShort = doc.getString("locationShort") ?: existing.locationShort,
                        latitude = doc.getDouble("latitude") ?: existing.latitude,
                        longitude = doc.getDouble("longitude") ?: existing.longitude,
                        syncedToFirebase = true
                    )
                    localRepository.updateScheduledTransaction(updated)
                } else {
                    val existing = localRepository.getTransactionByFirestoreId(firestoreId) ?: return
                    val localPhotoPath = handlePhotoUpdate(doc, existing.photoUri, firestoreId, isScheduled)

                    val updated = existing.copy(
                        amount = doc.getDouble("amount") ?: existing.amount,
                        transaction = TransactionType.valueOf(doc.getString("transaction") ?: existing.transaction.name),
                        note = doc.getString("note") ?: existing.note,
                        date = doc.getLong("date") ?: existing.date,
                        category = CategoryType.valueOf(doc.getString("category") ?: existing.category.name),
                        photoUri = localPhotoPath,
                        locationFull = doc.getString("locationFull") ?: existing.locationFull,
                        locationShort = doc.getString("locationShort") ?: existing.locationShort,
                        latitude = doc.getDouble("latitude") ?: existing.latitude,
                        longitude = doc.getDouble("longitude") ?: existing.longitude,
                        syncedToFirebase = true
                    )
                    localRepository.updateTransaction(updated)
                }
            }

            DocumentChange.Type.REMOVED -> {
                if (isScheduled) {
                    localRepository.getScheduledTransactionByFirestoreId(firestoreId)?.let {
                        localRepository.deleteScheduledTransaction(it)
                        WorkManager.getInstance(context).cancelAllWorkByTag("scheduled_notification_${it.id}")
                        WorkManager.getInstance(context).cancelAllWorkByTag("delete_expired_${it.id}")
                    }
                } else {
                    localRepository.getTransactionByFirestoreId(firestoreId)?.let {
                        localRepository.deleteTransaction(it)
                    }
                }
            }
        }
    }

    private suspend fun handlePhotoDownloadAndInsert(doc: DocumentSnapshot, firestoreId: String, isScheduled: Boolean) {
        val storagePhotoUrl = doc.getString("photoStorageUrl")
        val localPhotoPath: String? = null

        if (!storagePhotoUrl.isNullOrBlank()) {
            scope.launch {
                try {
                    val downloadResult = photoStorageManager.downloadAndSavePhoto(
                        context = context,
                        storageUrl = storagePhotoUrl,
                        firestoreId = firestoreId
                    )
                    if (downloadResult.isSuccess) {
                        val downloadedPath = downloadResult.getOrNull()
                        if (isScheduled) {
                            localRepository.getScheduledTransactionByFirestoreId(firestoreId)?.let {
                                localRepository.updateScheduledTransaction(it.copy(photoUri = downloadedPath))
                            }
                        } else {
                            localRepository.getTransactionByFirestoreId(firestoreId)?.let {
                                localRepository.updateTransaction(it.copy(photoUri = downloadedPath))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading photo", e)
                }
            }
        }

        if (isScheduled) {
            val entity = ScheduledTransactionEntity(
                firestoreId = firestoreId,
                amount = doc.getDouble("amount") ?: 0.0,
                type = TransactionType.valueOf(doc.getString("type") ?: TransactionType.EXPENSE.name),
                category = CategoryType.valueOf(doc.getString("category") ?: CategoryType.OTHER.name),
                note = doc.getString("note"),
                scheduledDate = doc.getLong("scheduledDate") ?: System.currentTimeMillis(),
                expirationNotificationSent = doc.getBoolean("expirationNotificationSent") ?: false,
                notificationSent = doc.getBoolean("notificationSent") ?: false,
                photoUri = localPhotoPath,
                locationFull = doc.getString("locationFull"),
                locationShort = doc.getString("locationShort"),
                latitude = doc.getDouble("latitude"),
                longitude = doc.getDouble("longitude"),
                syncedToFirebase = true
            )
            localRepository.insertScheduledTransaction(entity)
        } else {
            val entity = TransactionEntity(
                firestoreId = firestoreId,
                amount = doc.getDouble("amount") ?: 0.0,
                transaction = TransactionType.valueOf(doc.getString("transaction") ?: TransactionType.EXPENSE.name),
                note = doc.getString("note") ?: "",
                date = doc.getLong("date") ?: System.currentTimeMillis(),
                category = CategoryType.valueOf(doc.getString("category") ?: CategoryType.OTHER.name),
                photoUri = localPhotoPath,
                locationFull = doc.getString("locationFull"),
                locationShort = doc.getString("locationShort"),
                latitude = doc.getDouble("latitude"),
                longitude = doc.getDouble("longitude"),
                syncedToFirebase = true
            )
            localRepository.insertTransaction(entity)
        }
    }

    private fun handlePhotoUpdate(doc: DocumentSnapshot, currentPhotoUri: String?, firestoreId: String, isScheduled: Boolean): String? {
        val storagePhotoUrl = doc.getString("photoStorageUrl")
        var localPhotoPath = currentPhotoUri
        if (storagePhotoUrl == null && currentPhotoUri != null) {
            try {
                localPhotoPath = null
            } catch (e: Exception) {
                Log.e(TAG, "Error handling photo deletion", e)
            }
        }
        else if (!storagePhotoUrl.isNullOrBlank() && storagePhotoUrl != currentPhotoUri) {
            scope.launch {
                try {
                    val downloadResult = photoStorageManager.downloadAndSavePhoto(
                        context = context,
                        storageUrl = storagePhotoUrl,
                        firestoreId = firestoreId
                    )
                    if (downloadResult.isSuccess) {
                        localPhotoPath = downloadResult.getOrNull()
                        if (isScheduled) {
                            localRepository.getScheduledTransactionByFirestoreId(firestoreId)?.let {
                                localRepository.updateScheduledTransaction(it.copy(photoUri = localPhotoPath))
                            }
                        } else {
                            localRepository.getTransactionByFirestoreId(firestoreId)?.let {
                                localRepository.updateTransaction(it.copy(photoUri = localPhotoPath))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading photo", e)
                }
            }
        }
        return localPhotoPath
    }
    // --- NOTIFICATION ---
    private suspend fun sendPendingNotifications() {
        try {
            val token = try { messaging.token.await() } catch (e: Exception) { null } ?: return
            Firebase.functions.getHttpsCallable("sendPendingNotifications")
                .call(hashMapOf("deviceToken" to token))
        } catch (_: Exception) { }
    }

    fun stopListening() {
        transactionListener?.remove()
        scheduledListener?.remove()
        transactionListener = null
        scheduledListener = null
    }
}
package com.ahmetkaragunlu.financeai.firebasesync

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class FirebaseSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val localRepository: FinanceRepository,
    private val photoStorageManager: PhotoStorageManager,
    @ApplicationContext private val context: Context
) {
    private var transactionListener: ListenerRegistration? = null
    private var scheduledListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false

    // ✅ YENİ: Bu cihazda eklenen firestoreId'leri takip et
    private val recentlyAddedIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val SCHEDULED_COLLECTION = "scheduled_transactions"
        private const val RECENTLY_ADDED_TIMEOUT = 3000L // 3 saniye
    }

    private fun getUserId(): String? = auth.currentUser?.uid

    fun initializeSyncAfterLogin() {
        scope.launch {
            val userId = getUserId()
            if (userId == null) {
                Log.w(TAG, "Cannot initialize sync - user not logged in")
                return@launch
            }

            if (isInitialized) {
                Log.d(TAG, "Sync already initialized, skipping...")
                return@launch
            }

            Log.d(TAG, "Initializing sync for user: $userId")

            try {
                performInitialSync()
                delay(300)
                startListeningToTransactions()
                startListeningToScheduledTransactions()
                isInitialized = true
                Log.d(TAG, "Sync initialization completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync initialization", e)
            }
        }
    }

    fun resetSync() {
        Log.d(TAG, "Resetting sync...")
        stopListening()
        recentlyAddedIds.clear()
        isInitialized = false
    }

    // ✅ YENİ: Eklenen ID'yi takip et ve timeout sonrası kaldır
    private fun markAsRecentlyAdded(firestoreId: String) {
        recentlyAddedIds.add(firestoreId)
        scope.launch {
            delay(RECENTLY_ADDED_TIMEOUT)
            recentlyAddedIds.remove(firestoreId)
            Log.d(TAG, "🕐 Removed from recently added: $firestoreId")
        }
    }

    private fun scheduleNotificationForTransaction(transactionId: Long, scheduledDate: Long) {
        val currentTime = System.currentTimeMillis()

        if (scheduledDate > currentTime) {
            Log.d(TAG, "🔔 Starting WorkManager for scheduled transaction: $transactionId")

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(NotificationWorker.TRANSACTION_ID_KEY to transactionId)
                )
                .addTag("scheduled_notification_$transactionId")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "✅ WorkManager scheduled for transaction: $transactionId")
        } else {
            Log.d(TAG, "⏭️ Scheduled date is in the past, skipping WorkManager for: $transactionId")
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

            // ✅ YENİ: Bu ID'yi "recently added" olarak işaretle
            markAsRecentlyAdded(firestoreId)

            val currentTimestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "userId" to userId,
                "amount" to transaction.amount,
                "transaction" to transaction.transaction.name,
                "note" to transaction.note,
                "date" to transaction.date,
                "category" to transaction.category.name,
                "photoStorageUrl" to null,
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to currentTimestamp
            )
            docRef.set(data).await()
            Log.d(TAG, "Transaction synced to Firebase (without photo): $firestoreId")

            if (!transaction.photoUri.isNullOrBlank() && transaction.photoUri.startsWith("/")) {
                scope.launch {
                    try {
                        val uploadResult = photoStorageManager.uploadTransactionPhoto(
                            localPhotoPath = transaction.photoUri,
                            firestoreId = firestoreId
                        )

                        if (uploadResult.isSuccess) {
                            val storagePhotoUrl = uploadResult.getOrNull()
                            docRef.update("photoStorageUrl", storagePhotoUrl).await()
                            Log.d(TAG, "Photo uploaded and Firestore updated: $firestoreId")
                        } else {
                            Log.e(TAG, "Failed to upload photo", uploadResult.exceptionOrNull())
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading photo in background", e)
                    }
                }
            }

            Result.success(firestoreId)

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing transaction", e)
            Result.failure(e)
        }
    }

    suspend fun syncScheduledTransactionToFirebase(
        transaction: ScheduledTransactionEntity
    ): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))

            val docRef = if (transaction.firestoreId.isNotEmpty()) {
                firestore.collection(SCHEDULED_COLLECTION).document(transaction.firestoreId)
            } else {
                firestore.collection(SCHEDULED_COLLECTION).document()
            }
            val firestoreId = docRef.id

            // ✅ YENİ: Bu ID'yi "recently added" olarak işaretle
            markAsRecentlyAdded(firestoreId)

            val currentTimestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "userId" to userId,
                "amount" to transaction.amount,
                "type" to transaction.type.name,
                "category" to transaction.category.name,
                "note" to transaction.note,
                "scheduledDate" to transaction.scheduledDate,
                "expirationNotificationSent" to transaction.expirationNotificationSent,
                "notificationSent" to transaction.notificationSent,
                "photoStorageUrl" to null,
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to currentTimestamp
            )
            docRef.set(data).await()
            Log.d(TAG, "Scheduled synced to Firebase (without photo): $firestoreId")

            if (!transaction.photoUri.isNullOrBlank() && transaction.photoUri.startsWith("/")) {
                scope.launch {
                    try {
                        val uploadResult = photoStorageManager.uploadScheduledPhoto(
                            localPhotoPath = transaction.photoUri,
                            firestoreId = firestoreId
                        )

                        if (uploadResult.isSuccess) {
                            val storagePhotoUrl = uploadResult.getOrNull()
                            docRef.update("photoStorageUrl", storagePhotoUrl).await()
                            Log.d(TAG, "Scheduled photo uploaded: $firestoreId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading scheduled photo in background", e)
                    }
                }
            }

            Result.success(firestoreId)

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing scheduled transaction", e)
            Result.failure(e)
        }
    }

    suspend fun deleteScheduledTransactionFromFirebase(firestoreId: String): Result<Unit> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            Log.d(TAG, "Deleting from Firebase - firestoreId: $firestoreId")

            val doc = firestore.collection(SCHEDULED_COLLECTION).document(firestoreId).get().await()
            val photoUrl = doc.getString("photoStorageUrl")

            if (!photoUrl.isNullOrBlank()) {
                scope.launch {
                    try {
                        photoStorageManager.deletePhoto(photoUrl)
                        Log.d(TAG, "Photo deleted from Storage: $firestoreId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting photo", e)
                    }
                }
            }
            firestore.collection(SCHEDULED_COLLECTION).document(firestoreId).delete().await()
            Log.d(TAG, "Successfully deleted from Firebase: $firestoreId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting scheduled transaction from Firebase", e)
            Result.failure(e)
        }
    }

    private suspend fun performInitialSync() {
        val userId = getUserId() ?: return

        try {
            Log.d(TAG, "Starting initial sync from Firebase for user: $userId")

            val transactionsJob = scope.async { fetchAllTransactionsFromFirebase(userId) }
            val scheduledJob = scope.async { fetchAllScheduledTransactionsFromFirebase(userId) }

            transactionsJob.await()
            scheduledJob.await()

            Log.d(TAG, "Initial sync completed!")
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync", e)
        }
    }

    private suspend fun fetchAllScheduledTransactionsFromFirebase(userId: String) {
        try {
            val snapshot = firestore.collection(SCHEDULED_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d(TAG, "Fetched ${snapshot.documents.size} scheduled transactions from Firebase")

            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id
                val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)

                if (existing == null) {
                    val storagePhotoUrl = doc.getString("photoStorageUrl")
                    var localPhotoPath: String? = null

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
                                    val existingTx = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                    existingTx?.let {
                                        localRepository.updateScheduledTransaction(it.copy(photoUri = downloadedPath))
                                        Log.d(TAG, "Photo downloaded for scheduled: $firestoreId")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error downloading photo", e)
                            }
                        }
                    }

                    val scheduledTransaction = ScheduledTransactionEntity(
                        firestoreId = firestoreId,
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = TransactionType.valueOf(doc.getString("type") ?: "EXPENSE"),
                        category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
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
                    val localId = localRepository.insertScheduledTransaction(scheduledTransaction)

                    scheduleNotificationForTransaction(localId, scheduledTransaction.scheduledDate)
                    Log.d(TAG, "Restored scheduled transaction: $firestoreId (localId=$localId)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching scheduled transactions from Firebase", e)
        }
    }



    // ============================================
// ÖNEMLİ BÖLÜM 1: fetchAllTransactionsFromFirebase
// ============================================
    private suspend fun fetchAllTransactionsFromFirebase(userId: String) {
        try {
            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d(TAG, "Fetched ${snapshot.documents.size} transactions from Firebase")

            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id

                // ✅ ÇÖZÜM: Önce Room'da var mı kontrol et
                val existing = localRepository.getTransactionByFirestoreId(firestoreId)

                if (existing == null) {
                    Log.d(TAG, "➕ Restoring NEW transaction: $firestoreId")

                    val storagePhotoUrl = doc.getString("photoStorageUrl")
                    var localPhotoPath: String? = null

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
                                    val existingTx = localRepository.getTransactionByFirestoreId(firestoreId)
                                    existingTx?.let {
                                        localRepository.updateTransaction(it.copy(photoUri = downloadedPath))
                                        Log.d(TAG, "Photo downloaded for transaction: $firestoreId")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error downloading photo", e)
                            }
                        }
                    }

                    val transaction = TransactionEntity(
                        firestoreId = firestoreId,
                        amount = doc.getDouble("amount") ?: 0.0,
                        transaction = TransactionType.valueOf(doc.getString("transaction") ?: "EXPENSE"),
                        note = doc.getString("note") ?: "",
                        date = doc.getLong("date") ?: System.currentTimeMillis(),
                        category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
                        photoUri = localPhotoPath,
                        locationFull = doc.getString("locationFull"),
                        locationShort = doc.getString("locationShort"),
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        syncedToFirebase = true
                    )
                    localRepository.insertTransaction(transaction)
                    Log.d(TAG, "✅ Restored transaction: $firestoreId")
                } else {
                    Log.d(TAG, "⏭️ Transaction ALREADY EXISTS in Room, SKIPPING: $firestoreId (id=${existing.id})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions from Firebase", e)
        }
    }

    // ============================================
// ÖNEMLİ BÖLÜM 2: startListeningToTransactions
// ============================================
    private fun startListeningToTransactions() {
        val userId = getUserId() ?: return
        transactionListener?.remove()

        Log.d(TAG, "Starting transaction listener...")

        transactionListener = firestore.collection(TRANSACTIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening transactions", error)
                    return@addSnapshotListener
                }

                // 🔥 ÖNEMLİ: Sadece server'dan gelen değişiklikleri işle
                if (snapshot?.metadata?.isFromCache == false && snapshot.metadata.hasPendingWrites() == false) {
                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                val doc = change.document
                                val firestoreId = doc.id

                                when (change.type) {
                                    DocumentChange.Type.ADDED -> {
                                        // 🔥 ÇÖZÜM 1: Recently added kontrolü
                                        if (recentlyAddedIds.contains(firestoreId)) {
                                            Log.d(TAG, "⏭️ Skipping recently added transaction: $firestoreId")
                                            return@launch
                                        }

                                        // 🔥 ÇÖZÜM 2: Room'da var mı kontrol et
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)
                                        if (existing != null) {
                                            Log.d(TAG, "⏭️ Transaction ALREADY EXISTS in Room, SKIPPING: $firestoreId (id=${existing.id})")
                                            return@launch
                                        }

                                        Log.d(TAG, "➕ Adding NEW transaction from Firebase: $firestoreId")

                                        val storagePhotoUrl = doc.getString("photoStorageUrl")
                                        var localPhotoPath: String? = null

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
                                                        val existingTx = localRepository.getTransactionByFirestoreId(firestoreId)
                                                        existingTx?.let {
                                                            localRepository.updateTransaction(it.copy(photoUri = downloadedPath))
                                                            Log.d(TAG, "Photo downloaded for transaction: $firestoreId")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error downloading photo", e)
                                                }
                                            }
                                        }

                                        val transaction = TransactionEntity(
                                            firestoreId = firestoreId,
                                            amount = doc.getDouble("amount") ?: 0.0,
                                            transaction = TransactionType.valueOf(doc.getString("transaction") ?: "EXPENSE"),
                                            note = doc.getString("note") ?: "",
                                            date = doc.getLong("date") ?: System.currentTimeMillis(),
                                            category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
                                            photoUri = localPhotoPath,
                                            locationFull = doc.getString("locationFull"),
                                            locationShort = doc.getString("locationShort"),
                                            latitude = doc.getDouble("latitude"),
                                            longitude = doc.getDouble("longitude"),
                                            syncedToFirebase = true
                                        )
                                        localRepository.insertTransaction(transaction)
                                        Log.d(TAG, "✅ Transaction added from listener: $firestoreId")
                                    }

                                    DocumentChange.Type.MODIFIED -> {
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)
                                        if (existing != null) {
                                            val storagePhotoUrl = doc.getString("photoStorageUrl")
                                            var localPhotoPath = existing.photoUri

                                            if (!storagePhotoUrl.isNullOrBlank() && storagePhotoUrl != existing.photoUri) {
                                                scope.launch {
                                                    try {
                                                        val downloadResult = photoStorageManager.downloadAndSavePhoto(
                                                            context = context,
                                                            storageUrl = storagePhotoUrl,
                                                            firestoreId = firestoreId
                                                        )
                                                        if (downloadResult.isSuccess) {
                                                            localPhotoPath = downloadResult.getOrNull()
                                                            val existingTx = localRepository.getTransactionByFirestoreId(firestoreId)
                                                            existingTx?.let {
                                                                localRepository.updateTransaction(it.copy(photoUri = localPhotoPath))
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error downloading photo", e)
                                                    }
                                                }
                                            }

                                            val transaction = existing.copy(
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
                                            localRepository.updateTransaction(transaction)
                                            Log.d(TAG, "✅ Transaction updated from Firebase: $firestoreId")
                                        }
                                    }

                                    DocumentChange.Type.REMOVED -> {
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)
                                        existing?.let {
                                            localRepository.deleteTransaction(it)
                                            Log.d(TAG, "✅ Transaction deleted from local: $firestoreId")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing transaction change", e)
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "⏭️ Skipping snapshot - from cache or has pending writes")
                }
            }
    }

    // ============================================
// ÖNEMLİ BÖLÜM 3: startListeningToScheduledTransactions
// ============================================
    private fun startListeningToScheduledTransactions() {
        val userId = getUserId() ?: return
        scheduledListener?.remove()

        Log.d(TAG, "Starting scheduled transaction listener...")

        scheduledListener = firestore.collection(SCHEDULED_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening scheduled transactions", error)
                    return@addSnapshotListener
                }

                if (snapshot?.metadata?.isFromCache == false && snapshot.metadata.hasPendingWrites() == false) {
                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                val doc = change.document
                                val firestoreId = doc.id

                                when (change.type) {
                                    DocumentChange.Type.ADDED -> {
                                        // ✅ ÇÖZÜM 1: Recently added kontrolü
                                        if (recentlyAddedIds.contains(firestoreId)) {
                                            Log.d(TAG, "⏭️ Skipping recently added scheduled: $firestoreId")
                                            return@launch
                                        }

                                        // ✅ ÇÖZÜM 2: Room'da var mı kontrol et
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                        if (existing != null) {
                                            Log.d(TAG, "⏭️ Scheduled ALREADY EXISTS in Room, SKIPPING: $firestoreId (localId=${existing.id})")
                                            return@launch
                                        }

                                        Log.d(TAG, "➕ Adding NEW scheduled from Firebase: $firestoreId")

                                        val storagePhotoUrl = doc.getString("photoStorageUrl")
                                        var localPhotoPath: String? = null

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
                                                        val existingTx = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                                        existingTx?.let {
                                                            localRepository.updateScheduledTransaction(it.copy(photoUri = downloadedPath))
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error downloading photo", e)
                                                }
                                            }
                                        }

                                        val scheduledTransaction = ScheduledTransactionEntity(
                                            firestoreId = firestoreId,
                                            amount = doc.getDouble("amount") ?: 0.0,
                                            type = TransactionType.valueOf(doc.getString("type") ?: "EXPENSE"),
                                            category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
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

                                        val localId = localRepository.insertScheduledTransaction(scheduledTransaction)
                                        Log.d(TAG, "✅ Scheduled added from Firebase: $firestoreId (localId=$localId)")

                                        scheduleNotificationForTransaction(localId, scheduledTransaction.scheduledDate)
                                    }

                                    DocumentChange.Type.MODIFIED -> {
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                        if (existing != null) {
                                            val storagePhotoUrl = doc.getString("photoStorageUrl")
                                            var localPhotoPath = existing.photoUri

                                            if (!storagePhotoUrl.isNullOrBlank() && storagePhotoUrl != existing.photoUri) {
                                                scope.launch {
                                                    try {
                                                        val downloadResult = photoStorageManager.downloadAndSavePhoto(
                                                            context = context,
                                                            storageUrl = storagePhotoUrl,
                                                            firestoreId = firestoreId
                                                        )
                                                        if (downloadResult.isSuccess) {
                                                            localPhotoPath = downloadResult.getOrNull()
                                                            val existingTx = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                                            existingTx?.let {
                                                                localRepository.updateScheduledTransaction(it.copy(photoUri = localPhotoPath))
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error downloading photo", e)
                                                    }
                                                }
                                            }

                                            val scheduledTransaction = existing.copy(
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
                                            localRepository.updateScheduledTransaction(scheduledTransaction)
                                            Log.d(TAG, "Scheduled updated from Firebase: $firestoreId")
                                        }
                                    }

                                    DocumentChange.Type.REMOVED -> {
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                        existing?.let {
                                            localRepository.deleteScheduledTransaction(it)

                                            WorkManager.getInstance(context).cancelAllWorkByTag("scheduled_notification_${it.id}")
                                            WorkManager.getInstance(context).cancelAllWorkByTag("delete_expired_${it.id}")

                                            Log.d(TAG, "Scheduled deleted from local: $firestoreId")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing scheduled change", e)
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "⏭️ Skipping snapshot - from cache or has pending writes")
                }
            }
    }
    fun stopListening() {
        transactionListener?.remove()
        scheduledListener?.remove()
        transactionListener = null
        scheduledListener = null
        Log.d(TAG, "Listeners stopped")
    }
}
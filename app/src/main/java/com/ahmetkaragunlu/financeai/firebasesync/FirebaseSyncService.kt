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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions // EKLENDİ
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

    fun getNewTransactionId(): String {
        return firestore.collection(TRANSACTIONS_COLLECTION).document().id
    }

    fun getNewScheduledTransactionId(): String {
        return firestore.collection(SCHEDULED_COLLECTION).document().id
    }

    private fun getUserId(): String? = auth.currentUser?.uid

    private suspend fun sendPendingNotifications() {
        try {
            val currentToken = try {
                messaging.token.await()
            } catch (e: Exception) {
                null
            }
            if (currentToken == null) {
                return
            }
            val functions = Firebase.functions
            val data = hashMapOf<String, Any>(
                "deviceToken" to currentToken
            )
            functions
                .getHttpsCallable("sendPendingNotifications")
                .call(data)

        } catch (e: Exception) {
        }
    }

    fun initializeSyncAfterLogin() {
        scope.launch {
            val userId = getUserId()
            if (userId == null) {
                return@launch
            }
            if (isInitialized) {
                return@launch
            }
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

    private suspend fun pushUnsyncedData() {
        try {
            val unsyncedTransactions = localRepository.getUnsyncedTransactions().firstOrNull() ?: emptyList()
            unsyncedTransactions.forEach { transaction ->
                syncTransactionToFirebase(transaction).onSuccess { firestoreId ->
                    localRepository.updateTransaction(transaction.copy(syncedToFirebase = true))
                }
            }

            val unsyncedScheduled = localRepository.getUnsyncedScheduledTransactions().firstOrNull() ?: emptyList()
            unsyncedScheduled.forEach { transaction ->
                syncScheduledTransactionToFirebase(transaction).onSuccess { firestoreId ->
                    localRepository.updateScheduledTransaction(transaction.copy(syncedToFirebase = true))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing unsynced data", e)
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

    // GÜNCELLENDİ: SetOptions.merge() eklendi ve photoStorageUrl map'ten çıkarıldı.
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

            val currentTimestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "userId" to userId,
                "amount" to transaction.amount,
                "transaction" to transaction.transaction.name,
                "note" to transaction.note,
                "date" to transaction.date,
                "category" to transaction.category.name,
                // photoStorageUrl ÇIKARILDI: Null gönderip var olanı ezmesin diye.
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to currentTimestamp
            )
            // Merge ile sadece değişen alanlar güncellenir
            docRef.set(data, SetOptions.merge()).await()
            Result.success(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // GÜNCELLENDİ: SetOptions.merge() eklendi
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
                // photoStorageUrl ÇIKARILDI
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to currentTimestamp
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(firestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // YENİ EKLENDİ: Sadece fotoğrafı silmek için
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

            // Firestore'dan URL alanını temizle
            docRef.update("photoStorageUrl", null).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionFromFirebase(firestoreId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(TRANSACTIONS_COLLECTION).document(firestoreId).get().await()
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

            firestore.collection(TRANSACTIONS_COLLECTION).document(firestoreId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteScheduledTransactionFromFirebase(firestoreId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(SCHEDULED_COLLECTION).document(firestoreId).get().await()
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
            firestore.collection(SCHEDULED_COLLECTION).document(firestoreId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun performInitialSync() {
        val userId = getUserId() ?: return
        try {
            val transactionsJob = scope.async { fetchAllTransactionsFromFirebase(userId) }
            val scheduledJob = scope.async { fetchAllScheduledTransactionsFromFirebase(userId) }
            transactionsJob.await()
            scheduledJob.await()
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
            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id
                val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                if (existing == null) {
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
                                    val existingTx =
                                        localRepository.getScheduledTransactionByFirestoreId(
                                            firestoreId
                                        )
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
                        expirationNotificationSent = doc.getBoolean("expirationNotificationSent")
                            ?: false,
                        notificationSent = doc.getBoolean("notificationSent") ?: false,
                        photoUri = localPhotoPath,
                        locationFull = doc.getString("locationFull"),
                        locationShort = doc.getString("locationShort"),
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        syncedToFirebase = true
                    )
                    localRepository.insertScheduledTransaction(scheduledTransaction)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching scheduled transactions from Firebase", e)
        }
    }

    private suspend fun fetchAllTransactionsFromFirebase(userId: String) {
        try {
            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id
                val existing = localRepository.getTransactionByFirestoreId(firestoreId)
                if (existing == null) {
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
                                    val existingTx =
                                        localRepository.getTransactionByFirestoreId(firestoreId)
                                    existingTx?.let {
                                        localRepository.updateTransaction(it.copy(photoUri = downloadedPath))
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
                        transaction = TransactionType.valueOf(
                            doc.getString("transaction") ?: "EXPENSE"
                        ),
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
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions from Firebase", e)
        }
    }

    private fun startListeningToTransactions() {
        val userId = getUserId() ?: return
        transactionListener?.remove()
        transactionListener = firestore.collection(TRANSACTIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot?.metadata?.isFromCache == false && !snapshot.metadata.hasPendingWrites()) {
                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                val doc = change.document
                                val firestoreId = doc.id
                                when (change.type) {
                                    DocumentChange.Type.ADDED -> {
                                        if (recentlyAddedIds.contains(firestoreId)) {
                                            return@launch
                                        }
                                        val existing =
                                            localRepository.getTransactionByFirestoreId(firestoreId)
                                        if (existing != null) {
                                            return@launch
                                        }
                                        val storagePhotoUrl = doc.getString("photoStorageUrl")
                                        val localPhotoPath: String? = null
                                        if (!storagePhotoUrl.isNullOrBlank()) {
                                            scope.launch {
                                                try {
                                                    val downloadResult =
                                                        photoStorageManager.downloadAndSavePhoto(
                                                            context = context,
                                                            storageUrl = storagePhotoUrl,
                                                            firestoreId = firestoreId
                                                        )
                                                    if (downloadResult.isSuccess) {
                                                        val downloadedPath =
                                                            downloadResult.getOrNull()
                                                        val existingTx =
                                                            localRepository.getTransactionByFirestoreId(
                                                                firestoreId
                                                            )
                                                        existingTx?.let {
                                                            localRepository.updateTransaction(
                                                                it.copy(
                                                                    photoUri = downloadedPath
                                                                )
                                                            )
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
                                            transaction = TransactionType.valueOf(
                                                doc.getString("transaction") ?: TransactionType.EXPENSE.name
                                            ),
                                            note = doc.getString("note") ?: "",
                                            date = doc.getLong("date")
                                                ?: System.currentTimeMillis(),
                                            category = CategoryType.valueOf(
                                                doc.getString("category") ?: CategoryType.OTHER.name
                                            ),
                                            photoUri = localPhotoPath,
                                            locationFull = doc.getString("locationFull"),
                                            locationShort = doc.getString("locationShort"),
                                            latitude = doc.getDouble("latitude"),
                                            longitude = doc.getDouble("longitude"),
                                            syncedToFirebase = true
                                        )
                                        localRepository.insertTransaction(transaction)
                                    }
                                    DocumentChange.Type.MODIFIED -> {
                                        val existing =
                                            localRepository.getTransactionByFirestoreId(firestoreId)
                                        if (existing != null) {
                                            val storagePhotoUrl = doc.getString("photoStorageUrl")
                                            var localPhotoPath = existing.photoUri

                                            // 1. FOTOĞRAF SİLİNMİŞ Mİ KONTROLÜ
                                            if (storagePhotoUrl == null && existing.photoUri != null) {
                                                // Firebase'de silinmişse local'de de sil
                                                try {
                                                    // Dosya yolunu kullanarak silme işlemini yap (PhotoStorageUtil kullanılabilir
                                                    // ama bu class içinde PhotoStorageManager var, dosya işlemi için context lazım)
                                                    // Basitçe: ViewModel veya başka bir yer dosya varlığını kontrol edecektir.
                                                    // Burada sadece DB kaydını null yapıyoruz.
                                                    localPhotoPath = null
                                                } catch(e:Exception){
                                                    Log.e(TAG, "Error handling photo deletion", e)
                                                }
                                            }
                                            // 2. FOTOĞRAF DEĞİŞMİŞ Mİ KONTROLÜ
                                            else if (!storagePhotoUrl.isNullOrBlank() && storagePhotoUrl != existing.photoUri) {
                                                scope.launch {
                                                    try {
                                                        val downloadResult = photoStorageManager.downloadAndSavePhoto(
                                                            context = context,
                                                            storageUrl = storagePhotoUrl,
                                                            firestoreId = firestoreId
                                                        )
                                                        if (downloadResult.isSuccess) {
                                                            localPhotoPath =
                                                                downloadResult.getOrNull()
                                                            val existingTx =
                                                                localRepository.getTransactionByFirestoreId(
                                                                    firestoreId
                                                                )
                                                            existingTx?.let {
                                                                localRepository.updateTransaction(
                                                                    it.copy(
                                                                        photoUri = localPhotoPath
                                                                    )
                                                                )
                                                            }
                                                        }

                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error downloading photo", e)
                                                    }
                                                }
                                            }

                                            val transaction = existing.copy(
                                                amount = doc.getDouble("amount") ?: existing.amount,
                                                transaction = TransactionType.valueOf(
                                                    doc.getString(
                                                        "transaction"
                                                    ) ?: existing.transaction.name
                                                ),
                                                note = doc.getString("note") ?: existing.note,
                                                date = doc.getLong("date") ?: existing.date,
                                                category = CategoryType.valueOf(
                                                    doc.getString("category")
                                                        ?: existing.category.name
                                                ),
                                                photoUri = localPhotoPath,
                                                locationFull = doc.getString("locationFull")
                                                    ?: existing.locationFull,
                                                locationShort = doc.getString("locationShort")
                                                    ?: existing.locationShort,
                                                latitude = doc.getDouble("latitude")
                                                    ?: existing.latitude,
                                                longitude = doc.getDouble("longitude")
                                                    ?: existing.longitude,
                                                syncedToFirebase = true
                                            )
                                            localRepository.updateTransaction(transaction)
                                        }
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)
                                        existing?.let {
                                            localRepository.deleteTransaction(it)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing transaction change", e)
                            }
                        }
                    }
                }
            }
    }

    private fun startListeningToScheduledTransactions() {
        val userId = getUserId() ?: return
        scheduledListener?.remove()
        scheduledListener = firestore.collection(SCHEDULED_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot?.metadata?.isFromCache == false && !snapshot.metadata.hasPendingWrites()) {
                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                val doc = change.document
                                val firestoreId = doc.id
                                when (change.type) {
                                    DocumentChange.Type.ADDED -> {
                                        if (recentlyAddedIds.contains(firestoreId)) {
                                            return@launch
                                        }
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(
                                            firestoreId
                                        )
                                        if (existing != null) {
                                            return@launch

                                        }
                                        val storagePhotoUrl = doc.getString("photoStorageUrl")
                                        val localPhotoPath: String? = null
                                        if (!storagePhotoUrl.isNullOrBlank()) {
                                            scope.launch {
                                                try {
                                                    val downloadResult =
                                                        photoStorageManager.downloadAndSavePhoto(
                                                            context = context,
                                                            storageUrl = storagePhotoUrl,
                                                            firestoreId = firestoreId
                                                        )
                                                    if (downloadResult.isSuccess) {
                                                        val downloadedPath =
                                                            downloadResult.getOrNull()
                                                        val existingTx =
                                                            localRepository.getScheduledTransactionByFirestoreId(
                                                                firestoreId
                                                            )
                                                        existingTx?.let {
                                                            localRepository.updateScheduledTransaction(
                                                                it.copy(photoUri = downloadedPath)
                                                            )
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
                                            type = TransactionType.valueOf(
                                                doc.getString("type") ?: TransactionType.EXPENSE.name
                                            ),
                                            category = CategoryType.valueOf(
                                                doc.getString("category") ?: CategoryType.OTHER.name
                                            ),
                                            note = doc.getString("note"),
                                            scheduledDate = doc.getLong("scheduledDate")
                                                ?: System.currentTimeMillis(),
                                            expirationNotificationSent = doc.getBoolean("expirationNotificationSent")
                                                ?: false,
                                            notificationSent = doc.getBoolean("notificationSent")
                                                ?: false,
                                            photoUri = localPhotoPath,
                                            locationFull = doc.getString("locationFull"),
                                            locationShort = doc.getString("locationShort"),
                                            latitude = doc.getDouble("latitude"),
                                            longitude = doc.getDouble("longitude"),
                                            syncedToFirebase = true
                                        )
                                        localRepository.insertScheduledTransaction(scheduledTransaction)
                                    }
                                    DocumentChange.Type.MODIFIED -> {
                                        val existing =
                                            localRepository.getScheduledTransactionByFirestoreId(
                                                firestoreId
                                            )
                                        if (existing != null) {
                                            val storagePhotoUrl = doc.getString("photoStorageUrl")
                                            var localPhotoPath = existing.photoUri

                                            // 1. FOTO SİLİNMİŞ Mİ
                                            if (storagePhotoUrl == null && existing.photoUri != null) {
                                                localPhotoPath = null
                                            }
                                            // 2. FOTO DEĞİŞMİŞ Mİ
                                            else if (!storagePhotoUrl.isNullOrBlank() && storagePhotoUrl != existing.photoUri) {
                                                scope.launch {
                                                    try {
                                                        val downloadResult = photoStorageManager.downloadAndSavePhoto(
                                                            context = context,
                                                            storageUrl = storagePhotoUrl,
                                                            firestoreId = firestoreId
                                                        )
                                                        if (downloadResult.isSuccess) {
                                                            localPhotoPath =
                                                                downloadResult.getOrNull()
                                                            val existingTx =
                                                                localRepository.getScheduledTransactionByFirestoreId(
                                                                    firestoreId
                                                                )
                                                            existingTx?.let {
                                                                localRepository.updateScheduledTransaction(
                                                                    it.copy(photoUri = localPhotoPath)
                                                                )
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error downloading photo", e)
                                                    }
                                                }
                                            }
                                            val scheduledTransaction = existing.copy(
                                                amount = doc.getDouble("amount") ?: existing.amount,
                                                type = TransactionType.valueOf(
                                                    doc.getString("type") ?: existing.type.name
                                                ),
                                                category = CategoryType.valueOf(
                                                    doc.getString("category")
                                                        ?: existing.category.name
                                                ),
                                                note = doc.getString("note") ?: existing.note,
                                                scheduledDate = doc.getLong("scheduledDate")
                                                    ?: existing.scheduledDate,
                                                expirationNotificationSent = doc.getBoolean("expirationNotificationSent")
                                                    ?: existing.expirationNotificationSent,
                                                notificationSent = doc.getBoolean("notificationSent")
                                                    ?: existing.notificationSent,
                                                photoUri = localPhotoPath,
                                                locationFull = doc.getString("locationFull")
                                                    ?: existing.locationFull,
                                                locationShort = doc.getString("locationShort")
                                                    ?: existing.locationShort,
                                                latitude = doc.getDouble("latitude")
                                                    ?: existing.latitude,
                                                longitude = doc.getDouble("longitude")
                                                    ?: existing.longitude,
                                                syncedToFirebase = true
                                            )
                                            localRepository.updateScheduledTransaction(
                                                scheduledTransaction)
                                        }
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        val existing =
                                            localRepository.getScheduledTransactionByFirestoreId(
                                                firestoreId
                                            )
                                        existing?.let {
                                            localRepository.deleteScheduledTransaction(it)
                                            WorkManager.getInstance(context)
                                                .cancelAllWorkByTag("scheduled_notification_${it.id}")
                                            WorkManager.getInstance(context)
                                                .cancelAllWorkByTag("delete_expired_${it.id}")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing scheduled change", e)
                            }
                        }
                    }
                }
            }
    }

    fun stopListening() {
        transactionListener?.remove()
        scheduledListener?.remove()
        transactionListener = null
        scheduledListener = null
    }
}
package com.ahmetkaragunlu.financeai.firebaserepo

import android.util.Log
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val localRepository: FinanceRepository
) {
    private var transactionListener: ListenerRegistration? = null
    private var scheduledListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Listener başlatıldı mı kontrolü
    private var isTransactionListenerStarted = false
    private var isScheduledListenerStarted = false

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val TRANSACTIONS_COLLECTION = "transactions"
        private const val SCHEDULED_COLLECTION = "scheduled_transactions"
        private const val SYNC_THRESHOLD_MS = 5000L
    }

    private fun getUserId(): String? = auth.currentUser?.uid

    // Transaction'ı Firebase'e kaydet
    suspend fun syncTransactionToFirebase(transaction: TransactionEntity): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))

            val currentTimestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "userId" to userId,
                "amount" to transaction.amount,
                "transaction" to transaction.transaction.name,
                "note" to transaction.note,
                "date" to transaction.date,
                "category" to transaction.category.name,
                "photoUri" to transaction.photoUri,
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to currentTimestamp
            )

            val docRef = if (transaction.firestoreId.isNotEmpty()) {
                firestore.collection(TRANSACTIONS_COLLECTION)
                    .document(transaction.firestoreId)
            } else {
                firestore.collection(TRANSACTIONS_COLLECTION).document()
            }

            docRef.set(data).await()

            localRepository.updateTransaction(
                transaction.copy(
                    firestoreId = docRef.id,
                    syncedToFirebase = true
                )
            )

            Log.d(TAG, "Transaction synced: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing transaction", e)
            Result.failure(e)
        }
    }

    // Scheduled Transaction'ı Firebase'e kaydet
    suspend fun syncScheduledTransactionToFirebase(transaction: ScheduledTransactionEntity): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))

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
                "photoUri" to transaction.photoUri,
                "locationFull" to transaction.locationFull,
                "locationShort" to transaction.locationShort,
                "latitude" to transaction.latitude,
                "longitude" to transaction.longitude,
                "timestamp" to currentTimestamp
            )

            val docRef = if (transaction.firestoreId.isNotEmpty()) {
                firestore.collection(SCHEDULED_COLLECTION)
                    .document(transaction.firestoreId)
            } else {
                firestore.collection(SCHEDULED_COLLECTION).document()
            }

            docRef.set(data).await()

            localRepository.updateScheduledTransaction(
                transaction.copy(
                    firestoreId = docRef.id,
                    syncedToFirebase = true
                )
            )

            Log.d(TAG, "Scheduled transaction synced: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing scheduled transaction", e)
            Result.failure(e)
        }
    }

    suspend fun deleteScheduledTransactionFromFirebase(firestoreId: String): Result<Unit> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))

            Log.d(TAG, "Deleting from Firebase - firestoreId: $firestoreId")

            firestore.collection(SCHEDULED_COLLECTION)
                .document(firestoreId)
                .delete()
                .await()

            Log.d(TAG, "Successfully deleted from Firebase: $firestoreId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting scheduled transaction from Firebase", e)
            Result.failure(e)
        }
    }

    // İlk yükleme: Firebase'deki TÜM verileri local'e çek
    suspend fun performInitialSync() {
        val userId = getUserId() ?: return

        try {
            Log.d(TAG, "Starting initial sync from Firebase...")

            val localTransactions = localRepository.getAllTransactions().first()
            val localScheduled = localRepository.getAllScheduledTransactions().first()

            if (localTransactions.isEmpty()) {
                Log.d(TAG, "Local transactions empty, fetching from Firebase...")
                fetchAllTransactionsFromFirebase(userId)
            } else {
                Log.d(TAG, "Local transactions exist (${localTransactions.size}), skipping initial fetch")
            }

            if (localScheduled.isEmpty()) {
                Log.d(TAG, "Local scheduled transactions empty, fetching from Firebase...")
                fetchAllScheduledTransactionsFromFirebase(userId)
            } else {
                Log.d(TAG, "Local scheduled transactions exist (${localScheduled.size}), skipping initial fetch")
            }

            Log.d(TAG, "Initial sync completed!")
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync", e)
        }
    }

    private suspend fun fetchAllTransactionsFromFirebase(userId: String) {
        try {
            val snapshot = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d(TAG, "Fetched ${snapshot.documents.size} transactions from Firebase")

            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id
                val existing = localRepository.getTransactionByFirestoreId(firestoreId)

                if (existing == null) {
                    val transaction = TransactionEntity(
                        firestoreId = firestoreId,
                        amount = doc.getDouble("amount") ?: 0.0,
                        transaction = TransactionType.valueOf(doc.getString("transaction") ?: "EXPENSE"),
                        note = doc.getString("note") ?: "",
                        date = doc.getLong("date") ?: System.currentTimeMillis(),
                        category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
                        photoUri = doc.getString("photoUri"),
                        locationFull = doc.getString("locationFull"),
                        locationShort = doc.getString("locationShort"),
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        syncedToFirebase = true
                    )
                    localRepository.insertTransaction(transaction)
                    Log.d(TAG, "Restored transaction: $firestoreId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions from Firebase", e)
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
                    val scheduledTransaction = ScheduledTransactionEntity(
                        firestoreId = firestoreId,
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = TransactionType.valueOf(doc.getString("type") ?: "EXPENSE"),
                        category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
                        note = doc.getString("note"),
                        scheduledDate = doc.getLong("scheduledDate") ?: System.currentTimeMillis(),
                        expirationNotificationSent = doc.getBoolean("expirationNotificationSent") ?: false,
                        notificationSent = doc.getBoolean("notificationSent") ?: false,
                        photoUri = doc.getString("photoUri"),
                        locationFull = doc.getString("locationFull"),
                        locationShort = doc.getString("locationShort"),
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        syncedToFirebase = true
                    )
                    localRepository.insertScheduledTransaction(scheduledTransaction)
                    Log.d(TAG, "Restored scheduled transaction: $firestoreId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching scheduled transactions from Firebase", e)
        }
    }

    // Firebase'den Transaction'ları dinle (sadece CANLI değişiklikler için)
    fun startListeningToTransactions() {
        val userId = getUserId() ?: return

        // Zaten listener başlatıldıysa tekrar başlatma
        if (isTransactionListenerStarted) {
            Log.d(TAG, "Transaction listener already started, skipping...")
            return
        }

        Log.d(TAG, "Starting transaction listener...")
        isTransactionListenerStarted = true

        transactionListener = firestore.collection(TRANSACTIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening transactions", error)
                    return@addSnapshotListener
                }

                // İlk snapshot'ı atla (zaten performInitialSync'de çektik)
                if (snapshot?.metadata?.isFromCache == false && snapshot.metadata.hasPendingWrites() == false) {

                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                val doc = change.document
                                val firestoreId = doc.id

                                when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                        // Sadece local'de yoksa ekle
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)

                                        if (existing == null) {
                                            val transaction = TransactionEntity(
                                                firestoreId = firestoreId,
                                                amount = doc.getDouble("amount") ?: 0.0,
                                                transaction = TransactionType.valueOf(doc.getString("transaction") ?: "EXPENSE"),
                                                note = doc.getString("note") ?: "",
                                                date = doc.getLong("date") ?: System.currentTimeMillis(),
                                                category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
                                                photoUri = doc.getString("photoUri"),
                                                locationFull = doc.getString("locationFull"),
                                                locationShort = doc.getString("locationShort"),
                                                latitude = doc.getDouble("latitude"),
                                                longitude = doc.getDouble("longitude"),
                                                syncedToFirebase = true
                                            )
                                            localRepository.insertTransaction(transaction)
                                            Log.d(TAG, "Transaction added from Firebase (other device): $firestoreId")
                                        } else {
                                            Log.d(TAG, "Transaction already exists locally: $firestoreId")
                                        }
                                    }

                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)

                                        if (existing != null) {
                                            val transaction = existing.copy(
                                                amount = doc.getDouble("amount") ?: existing.amount,
                                                transaction = TransactionType.valueOf(doc.getString("transaction") ?: existing.transaction.name),
                                                note = doc.getString("note") ?: existing.note,
                                                date = doc.getLong("date") ?: existing.date,
                                                category = CategoryType.valueOf(doc.getString("category") ?: existing.category.name),
                                                photoUri = doc.getString("photoUri") ?: existing.photoUri,
                                                locationFull = doc.getString("locationFull") ?: existing.locationFull,
                                                locationShort = doc.getString("locationShort") ?: existing.locationShort,
                                                latitude = doc.getDouble("latitude") ?: existing.latitude,
                                                longitude = doc.getDouble("longitude") ?: existing.longitude,
                                                syncedToFirebase = true
                                            )
                                            localRepository.updateTransaction(transaction)
                                            Log.d(TAG, "Transaction updated from Firebase: $firestoreId")
                                        }
                                    }

                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        val existing = localRepository.getTransactionByFirestoreId(firestoreId)
                                        existing?.let {
                                            localRepository.deleteTransaction(it)
                                            Log.d(TAG, "Transaction deleted from local: $firestoreId")
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

    // Firebase'den Scheduled Transaction'ları dinle
    fun startListeningToScheduledTransactions() {
        val userId = getUserId() ?: return

        if (isScheduledListenerStarted) {
            Log.d(TAG, "Scheduled transaction listener already started, skipping...")
            return
        }

        Log.d(TAG, "Starting scheduled transaction listener...")
        isScheduledListenerStarted = true

        scheduledListener = firestore.collection(SCHEDULED_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening scheduled transactions", error)
                    return@addSnapshotListener
                }

                // İlk snapshot'ı atla
                if (snapshot?.metadata?.isFromCache == false && snapshot.metadata.hasPendingWrites() == false) {

                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                val doc = change.document
                                val firestoreId = doc.id

                                when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)

                                        if (existing == null) {
                                            val scheduledTransaction = ScheduledTransactionEntity(
                                                firestoreId = firestoreId,
                                                amount = doc.getDouble("amount") ?: 0.0,
                                                type = TransactionType.valueOf(doc.getString("type") ?: "EXPENSE"),
                                                category = CategoryType.valueOf(doc.getString("category") ?: "OTHER"),
                                                note = doc.getString("note"),
                                                scheduledDate = doc.getLong("scheduledDate") ?: System.currentTimeMillis(),
                                                expirationNotificationSent = doc.getBoolean("expirationNotificationSent") ?: false,
                                                notificationSent = doc.getBoolean("notificationSent") ?: false,
                                                photoUri = doc.getString("photoUri"),
                                                locationFull = doc.getString("locationFull"),
                                                locationShort = doc.getString("locationShort"),
                                                latitude = doc.getDouble("latitude"),
                                                longitude = doc.getDouble("longitude"),
                                                syncedToFirebase = true
                                            )
                                            localRepository.insertScheduledTransaction(scheduledTransaction)
                                            Log.d(TAG, "Scheduled transaction added from Firebase (other device): $firestoreId")
                                        } else {
                                            Log.d(TAG, "Scheduled transaction already exists locally: $firestoreId")
                                        }
                                    }

                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)

                                        if (existing != null) {
                                            val scheduledTransaction = existing.copy(
                                                amount = doc.getDouble("amount") ?: existing.amount,
                                                type = TransactionType.valueOf(doc.getString("type") ?: existing.type.name),
                                                category = CategoryType.valueOf(doc.getString("category") ?: existing.category.name),
                                                note = doc.getString("note") ?: existing.note,
                                                scheduledDate = doc.getLong("scheduledDate") ?: existing.scheduledDate,
                                                expirationNotificationSent = doc.getBoolean("expirationNotificationSent") ?: existing.expirationNotificationSent,
                                                notificationSent = doc.getBoolean("notificationSent") ?: existing.notificationSent,
                                                photoUri = doc.getString("photoUri") ?: existing.photoUri,
                                                locationFull = doc.getString("locationFull") ?: existing.locationFull,
                                                locationShort = doc.getString("locationShort") ?: existing.locationShort,
                                                latitude = doc.getDouble("latitude") ?: existing.latitude,
                                                longitude = doc.getDouble("longitude") ?: existing.longitude,
                                                syncedToFirebase = true
                                            )
                                            localRepository.updateScheduledTransaction(scheduledTransaction)
                                            Log.d(TAG, "Scheduled transaction updated from Firebase: $firestoreId")
                                        }
                                    }

                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        val existing = localRepository.getScheduledTransactionByFirestoreId(firestoreId)
                                        existing?.let {
                                            localRepository.deleteScheduledTransaction(it)
                                            Log.d(TAG, "Scheduled transaction deleted from local: $firestoreId")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing scheduled transaction change", e)
                            }
                        }
                    }
                }
            }
    }

    fun stopListening() {
        transactionListener?.remove()
        scheduledListener?.remove()
        isTransactionListenerStarted = false
        isScheduledListenerStarted = false
        Log.d(TAG, "Listeners stopped")
    }
}
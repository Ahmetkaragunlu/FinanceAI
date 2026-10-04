package com.ahmetkaragunlu.financeai.firebasesync

import com.ahmetkaragunlu.financeai.feature.transaction.domain.sync.TransactionSync
import com.ahmetkaragunlu.financeai.feature.budget.domain.sync.BudgetSync
import com.ahmetkaragunlu.financeai.feature.schedule.domain.sync.ScheduledTransactionSync
import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageDao
import com.ahmetkaragunlu.financeai.feature.aichat.data.local.AiMessageEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.notification.NotificationWorker
import com.ahmetkaragunlu.financeai.photo.PhotoStorageManager
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
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val localRepository: TransactionRepository,
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val aiMessageDao: AiMessageDao,
    private val photoStorageManager: PhotoStorageManager,
    private val messaging: FirebaseMessaging,
    @ApplicationContext private val context: Context
) : TransactionSync, BudgetSync, ScheduledTransactionSync {
    private val activeListeners = mutableMapOf<SyncType, ListenerRegistration>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    private val recentlyAddedIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val RECENTLY_ADDED_TIMEOUT = 3000L
    }

    // --- ID GENERATION ---
    override fun createTransactionId(): String = generateId(SyncType.TRANSACTION)
    override fun createScheduledTransactionId(): String = generateId(SyncType.SCHEDULED)
    override fun createBudgetId(): String = generateId(SyncType.BUDGET)
    private fun generateId(type: SyncType): String =
        firestore.collection(type.collectionName).document().id

    private fun getUserId(): String? = auth.currentUser?.uid

    // --- INITIALIZATION ---
    fun initializeSyncAfterLogin() {
        scope.launch {
            if (getUserId() == null || isInitialized) return@launch
            try {
                performInitialSync()
                pushUnsyncedData()
                delay(300)
                startAllListeners()
                delay(500)
                sendPendingNotifications()
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync initialization (${e.javaClass.simpleName})")
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

    // --- GENERIC SYNC HELPER ---
    private suspend fun syncToFirebase(
        type: SyncType,
        firestoreId: String,
        data: Map<String, Any?>
    ): Result<String> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            val docRef = if (firestoreId.isNotEmpty()) {
                firestore.collection(type.collectionName).document(firestoreId)
            } else {
                firestore.collection(type.collectionName).document()
            }
            val finalFirestoreId = docRef.id
            markAsRecentlyAdded(finalFirestoreId)
            val finalData = data.toMutableMap().apply {
                put("userId", userId)
                put("timestamp", System.currentTimeMillis())
            }
            docRef.set(finalData, SetOptions.merge()).await()
            Result.success(finalFirestoreId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ENTITY TO FIREBASE MAP EXTENSIONS ---
    private fun Transaction.toFirebaseMap(): Map<String, Any?> = mapOf(
        "amount" to amount,
        "transaction" to transaction.name,
        "note" to note,
        "date" to date,
        "category" to category.name,
        "locationFull" to locationFull,
        "locationShort" to locationShort,
        "latitude" to latitude,
        "longitude" to longitude
    )
    private fun ScheduledTransaction.toFirebaseMap(): Map<String, Any?> = mapOf(
        "amount" to amount,
        "type" to type.name,
        "category" to category.name,
        "note" to note,
        "scheduledDate" to scheduledDate,
        "expirationNotificationSent" to expirationNotificationSent,
        "notificationSent" to notificationSent,
        "locationFull" to locationFull,
        "locationShort" to locationShort,
        "latitude" to latitude,
        "longitude" to longitude
    )
    private fun Budget.toFirebaseMap(): Map<String, Any?> = mapOf(
        "budgetType" to budgetType.name,
        "category" to category?.name,
        "amount" to amount,
        "limitPercentage" to limitPercentage
    )
    private fun AiMessageEntity.toFirebaseMap(): Map<String, Any?> = mapOf(
        "text" to text,
        "isAi" to isAi,
        "timestamp" to timestamp.time
    )

    // --- PUBLIC SYNC METHODS ---
    override suspend fun syncTransaction(transaction: Transaction): Result<String> =
        syncToFirebase(SyncType.TRANSACTION, transaction.firestoreId, transaction.toFirebaseMap())
    override suspend fun syncScheduledTransaction(transaction: ScheduledTransaction): Result<String> =
        syncToFirebase(SyncType.SCHEDULED, transaction.firestoreId, transaction.toFirebaseMap())

    override suspend fun syncBudget(budget: Budget): Result<String> =
        syncToFirebase(SyncType.BUDGET, budget.firestoreId, budget.toFirebaseMap())

    suspend fun syncAiMessageToFirebase(message: AiMessageEntity): Result<String> =
        syncToFirebase(SyncType.AI_MESSAGE, message.firebaseId ?: "", message.toFirebaseMap())

    // --- PUSH UNSYNCED DATA ---
    private suspend fun pushUnsyncedData() {
        try {
            pushUnsyncedTransactions()
            pushUnsyncedScheduledTransactions()
            pushUnsyncedBudgets()
            pushUnsyncedAiMessages()
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing unsynced data (${e.javaClass.simpleName})")
        }
    }
    private suspend fun pushUnsyncedTransactions() {
        localRepository.observeUnsyncedTransactions().firstOrNull()?.forEach { transaction ->
            syncTransaction(transaction).onSuccess {
                localRepository.updateTransaction(transaction.copy(syncedToFirebase = true))
            }
        }
    }
    private suspend fun pushUnsyncedScheduledTransactions() {
        scheduledTransactionRepository.observeUnsyncedScheduledTransactions().firstOrNull()?.forEach { transaction ->
            syncScheduledTransaction(transaction).onSuccess {
                scheduledTransactionRepository.updateScheduledTransaction(transaction.copy(syncedToFirebase = true))
            }
        }
    }
    private suspend fun pushUnsyncedBudgets() {
        budgetRepository.observeUnsyncedBudgets().firstOrNull()?.forEach { budget ->
            syncBudget(budget).onSuccess {
                budgetRepository.updateBudget(budget.copy(syncedToFirebase = true))
            }
        }
    }
    private suspend fun pushUnsyncedAiMessages() {
        aiMessageDao.getUnsyncedMessages().forEach { message ->
            syncAiMessageToFirebase(message).onSuccess { firestoreId ->
                aiMessageDao.updateSyncStatus(message.id, firestoreId)
            }
        }
    }
    // --- DOCUMENT TO ENTITY MAPPERS (WITH EXISTING FALLBACK) ---
    private fun DocumentSnapshot.toTransaction(
        firestoreId: String,
        photoUri: String? = null,
        existing: Transaction? = null
    ): Transaction {
        return Transaction(
            firestoreId = firestoreId,
            amount = getDouble("amount") ?: existing?.amount ?: 0.0,
            transaction = TransactionType.valueOf(
                getString("transaction") ?: existing?.transaction?.name
                ?: TransactionType.EXPENSE.name
            ),
            note = getString("note") ?: existing?.note ?: "",
            date = getLong("date") ?: existing?.date ?: System.currentTimeMillis(),
            category = CategoryType.valueOf(
                getString("category") ?: existing?.category?.name ?: CategoryType.OTHER.name
            ),
            photoUri = photoUri,
            locationFull = getString("locationFull") ?: existing?.locationFull,
            locationShort = getString("locationShort") ?: existing?.locationShort,
            latitude = getDouble("latitude") ?: existing?.latitude,
            longitude = getDouble("longitude") ?: existing?.longitude,
            syncedToFirebase = true
        )
    }
    private fun DocumentSnapshot.toScheduledTransaction(
        firestoreId: String,
        photoUri: String? = null,
        existing: ScheduledTransaction? = null
    ): ScheduledTransaction {
        return ScheduledTransaction(
            firestoreId = firestoreId,
            amount = getDouble("amount") ?: existing?.amount ?: 0.0,
            type = TransactionType.valueOf(
                getString("type") ?: existing?.type?.name ?: TransactionType.EXPENSE.name
            ),
            category = CategoryType.valueOf(
                getString("category") ?: existing?.category?.name ?: CategoryType.OTHER.name
            ),
            note = getString("note") ?: existing?.note,
            scheduledDate = getLong("scheduledDate") ?: existing?.scheduledDate
            ?: System.currentTimeMillis(),
            expirationNotificationSent = getBoolean("expirationNotificationSent")
                ?: existing?.expirationNotificationSent ?: false,
            notificationSent = getBoolean("notificationSent") ?: existing?.notificationSent
            ?: false,
            photoUri = photoUri,
            locationFull = getString("locationFull") ?: existing?.locationFull,
            locationShort = getString("locationShort") ?: existing?.locationShort,
            latitude = getDouble("latitude") ?: existing?.latitude,
            longitude = getDouble("longitude") ?: existing?.longitude,
            syncedToFirebase = true
        )
    }
    private fun DocumentSnapshot.toBudget(
        firestoreId: String,
        existing: Budget? = null
    ): Budget {
        return Budget(
            firestoreId = firestoreId,
            budgetType = BudgetType.valueOf(
                getString("budgetType") ?: existing?.budgetType?.name
                ?: BudgetType.CATEGORY_AMOUNT.name
            ),
            category = getString("category")?.let { CategoryType.valueOf(it) }
                ?: existing?.category,
            amount = getDouble("amount") ?: existing?.amount ?: 0.0,
            limitPercentage = getDouble("limitPercentage") ?: existing?.limitPercentage,
            syncedToFirebase = true
        )
    }
    private fun DocumentSnapshot.toAiMessageEntity(firestoreId: String): AiMessageEntity {
        val timestampLong = getLong("timestamp") ?: System.currentTimeMillis()
        return AiMessageEntity(
            firebaseId = firestoreId,
            text = getString("text") ?: "",
            isAi = getBoolean("isAi") ?: false,
            timestamp = Date(timestampLong),
            isSynced = true
        )
    }
    // --- DELETE OPERATIONS ---
    override suspend fun deleteTransactionPhoto(firestoreId: String): Result<Unit> {
        return try {
            val docRef =
                firestore.collection(SyncType.TRANSACTION.collectionName).document(firestoreId)
            val doc = docRef.get().await()
            deletePhotoIfExists(doc.getString("photoStorageUrl"))
            docRef.update("photoStorageUrl", null).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun deleteTransaction(firestoreId: String): Result<Unit> =
        deleteDocumentWithPhoto(SyncType.TRANSACTION, firestoreId)
    override suspend fun deleteBudget(firestoreId: String): Result<Unit> {
        return try {
            firestore.collection(SyncType.BUDGET.collectionName).document(firestoreId).delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun deleteScheduledTransaction(firestoreId: String): Result<Unit> {
        return try {
            deletePhotoFromDocument(SyncType.SCHEDULED, firestoreId)
            deleteNotificationReminders(firestoreId)
            firestore.collection(SyncType.SCHEDULED.collectionName).document(firestoreId).delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteDocumentWithPhoto(type: SyncType, firestoreId: String): Result<Unit> {
        return try {
            deletePhotoFromDocument(type, firestoreId)
            firestore.collection(type.collectionName).document(firestoreId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private suspend fun deletePhotoFromDocument(type: SyncType, firestoreId: String) {
        try {
            val doc = firestore.collection(type.collectionName).document(firestoreId).get().await()
            deletePhotoIfExists(doc.getString("photoStorageUrl"))
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting photo from document (${e.javaClass.simpleName})")
        }
    }

    private fun deletePhotoIfExists(photoUrl: String?) {
        if (!photoUrl.isNullOrBlank()) {
            scope.launch {
                try {
                    photoStorageManager.deletePhoto(photoUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting photo (${e.javaClass.simpleName})")
                }
            }
        }
    }
    private suspend fun deleteNotificationReminders(firestoreId: String) {
        try {
            val remindersQuery = firestore.collection("notification_reminders")
                .whereEqualTo("transactionId", firestoreId)
                .get()
                .await()
            if (!remindersQuery.isEmpty) {
                val batch = firestore.batch()
                remindersQuery.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification reminders (${e.javaClass.simpleName})")
        }
    }

    // --- INITIAL SYNC ---
    private suspend fun performInitialSync() {
        val userId = getUserId() ?: return
        try {
            val jobs = SyncType.entries.map { type ->
                scope.async { fetchAllFromFirebase(type, userId) }
            }
            jobs.forEach { it.await() }
            scheduleNotificationsForPendingTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync (${e.javaClass.simpleName})")
        }
    }

    private suspend fun fetchAllFromFirebase(type: SyncType, userId: String) {
        try {
            val snapshot = firestore.collection(type.collectionName)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                val firestoreId = doc.id
                if (!checkEntityExists(firestoreId, type)) {
                    insertEntityFromDocument(doc, firestoreId, type)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Firebase: ${type.collectionName} (${e.javaClass.simpleName})")
        }
    }

    private suspend fun checkEntityExists(firestoreId: String, type: SyncType): Boolean {
        return when (type) {
            SyncType.TRANSACTION -> localRepository.getTransactionByFirestoreId(firestoreId) != null
            SyncType.SCHEDULED -> scheduledTransactionRepository.getScheduledTransactionByFirestoreId(firestoreId) != null
            SyncType.BUDGET -> budgetRepository.getBudgetByFirestoreId(firestoreId) != null
            SyncType.AI_MESSAGE -> aiMessageDao.getMessageByFirebaseId(firestoreId) != null
        }
    }

    private suspend fun insertEntityFromDocument(
        doc: DocumentSnapshot,
        firestoreId: String,
        type: SyncType
    ) {
        when (type) {
            SyncType.BUDGET -> budgetRepository.insertBudget(doc.toBudget(firestoreId))
            SyncType.AI_MESSAGE -> aiMessageDao.insertMessage(doc.toAiMessageEntity(firestoreId))
            SyncType.TRANSACTION, SyncType.SCHEDULED -> handlePhotoDownloadAndInsert(
                doc,
                firestoreId,
                type
            )
        }
    }
    private suspend fun handlePhotoDownloadAndInsert(
        doc: DocumentSnapshot,
        firestoreId: String,
        type: SyncType
    ) {
        val storagePhotoUrl = doc.getString("photoStorageUrl")
        downloadPhotoAsync(storagePhotoUrl, firestoreId, type)
        when (type) {
            SyncType.SCHEDULED -> scheduledTransactionRepository.insertScheduledTransaction(
                doc.toScheduledTransaction(firestoreId)
            )

            SyncType.TRANSACTION -> localRepository.insertTransaction(
                doc.toTransaction(firestoreId)
            )
            else -> {}
        }
    }

    // --- NOTIFICATION SCHEDULING ---
    private suspend fun scheduleNotificationsForPendingTransactions() {
        try {
            val allScheduled = scheduledTransactionRepository.observeScheduledTransactions().firstOrNull() ?: return
            val now = System.currentTimeMillis()
            allScheduled.forEach { transaction ->
                if (transaction.scheduledDate > now) {
                    scheduleNotificationWork(transaction.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notifications (${e.javaClass.simpleName})")
        }
    }

    private fun scheduleNotificationWork(transactionId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(workDataOf(NotificationWorker.TRANSACTION_ID_KEY to transactionId))
            .addTag("scheduled_notification_$transactionId")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "scheduled_notification_$transactionId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    // --- LISTENERS ---
    private fun startAllListeners() {
        val userId = getUserId() ?: return
        SyncType.entries.forEach { type ->
            activeListeners[type]?.remove()
            activeListeners[type] = createListener(type, userId)
        }
    }
    private fun createListener(type: SyncType, userId: String): ListenerRegistration {
        return firestore.collection(type.collectionName)
            .whereEqualTo("userId", userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                    snapshot.documentChanges.forEach { change ->
                        scope.launch {
                            try {
                                processDocumentChange(change, type)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing change for ${type.name} (${e.javaClass.simpleName})")
                            }
                        }
                    }
                }
            }
    }
    private suspend fun processDocumentChange(change: DocumentChange, type: SyncType) {
        val doc = change.document
        val firestoreId = doc.id
        when (change.type) {
            DocumentChange.Type.ADDED -> handleDocumentAdded(doc, firestoreId, type)
            DocumentChange.Type.MODIFIED -> handleDocumentModified(doc, firestoreId, type)
            DocumentChange.Type.REMOVED -> handleDocumentRemoved(firestoreId, type)
        }
    }

    private suspend fun handleDocumentAdded(
        doc: DocumentSnapshot,
        firestoreId: String,
        type: SyncType
    ) {
        if (recentlyAddedIds.contains(firestoreId)) return
        if (checkEntityExists(firestoreId, type)) return
        insertEntityFromDocument(doc, firestoreId, type)
    }
    private suspend fun handleDocumentModified(
        doc: DocumentSnapshot,
        firestoreId: String,
        type: SyncType
    ) {
        when (type) {
            SyncType.TRANSACTION -> updateTransaction(doc, firestoreId)
            SyncType.SCHEDULED -> updateScheduledTransaction(doc, firestoreId)
            SyncType.BUDGET -> updateBudget(doc, firestoreId)
            SyncType.AI_MESSAGE -> {}
        }
    }

    private suspend fun handleDocumentRemoved(firestoreId: String, type: SyncType) {
        when (type) {
            SyncType.TRANSACTION -> deleteLocalTransaction(firestoreId)
            SyncType.SCHEDULED -> deleteLocalScheduledTransaction(firestoreId)
            SyncType.BUDGET -> deleteLocalBudget(firestoreId)
            SyncType.AI_MESSAGE -> aiMessageDao.deleteMessageByFirebaseId(firestoreId)
        }
    }

    // --- UPDATE OPERATIONS (WITH EXISTING FALLBACK) ---
    private suspend fun updateTransaction(doc: DocumentSnapshot, firestoreId: String) {
        val existing = localRepository.getTransactionByFirestoreId(firestoreId) ?: return
        val localPhotoPath =
            handlePhotoUpdate(doc, existing.photoUri, firestoreId, SyncType.TRANSACTION)

        val updated = doc.toTransaction(firestoreId, localPhotoPath, existing)
        localRepository.updateTransaction(updated)
    }

    private suspend fun updateScheduledTransaction(doc: DocumentSnapshot, firestoreId: String) {
        val existing = scheduledTransactionRepository.getScheduledTransactionByFirestoreId(firestoreId) ?: return
        val localPhotoPath =
            handlePhotoUpdate(doc, existing.photoUri, firestoreId, SyncType.SCHEDULED)

        val updated = doc.toScheduledTransaction(firestoreId, localPhotoPath, existing)
        scheduledTransactionRepository.updateScheduledTransaction(updated)
    }

    private suspend fun updateBudget(doc: DocumentSnapshot, firestoreId: String) {
        val existing = budgetRepository.getBudgetByFirestoreId(firestoreId) ?: return

        val updated = doc.toBudget(firestoreId, existing)
        budgetRepository.updateBudget(updated)
    }

    // --- DELETE LOCAL OPERATIONS ---
    private suspend fun deleteLocalTransaction(firestoreId: String) {
        localRepository.getTransactionByFirestoreId(firestoreId)?.let {
            localRepository.deleteTransaction(it)
        }
    }

    private suspend fun deleteLocalScheduledTransaction(firestoreId: String) {
        scheduledTransactionRepository.getScheduledTransactionByFirestoreId(firestoreId)?.let {
            scheduledTransactionRepository.deleteScheduledTransaction(it)
            WorkManager.getInstance(context).apply {
                cancelAllWorkByTag("scheduled_notification_${it.id}")
                cancelAllWorkByTag("delete_expired_${it.id}")
            }
        }
    }

    private suspend fun deleteLocalBudget(firestoreId: String) {
        budgetRepository.getBudgetByFirestoreId(firestoreId)?.let {
            budgetRepository.deleteBudget(it)
        }
    }

    // --- PHOTO HANDLING ---
    private fun downloadPhotoAsync(storagePhotoUrl: String?, firestoreId: String, type: SyncType) {
        if (storagePhotoUrl.isNullOrBlank()) return

        scope.launch {
            try {
                val downloadResult = photoStorageManager.downloadAndSavePhoto(
                    context = context,
                    storageUrl = storagePhotoUrl,
                    firestoreId = firestoreId
                )
                if (downloadResult.isSuccess) {
                    val downloadedPath = downloadResult.getOrNull()
                    updateEntityWithPhoto(firestoreId, downloadedPath, type)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading photo (${e.javaClass.simpleName})")
            }
        }
    }
    private suspend fun updateEntityWithPhoto(
        firestoreId: String,
        photoPath: String?,
        type: SyncType
    ) {
        when (type) {
            SyncType.SCHEDULED -> {
                scheduledTransactionRepository.getScheduledTransactionByFirestoreId(firestoreId)?.let {
                    scheduledTransactionRepository.updateScheduledTransaction(it.copy(photoUri = photoPath))
                }
            }
            SyncType.TRANSACTION -> {
                localRepository.getTransactionByFirestoreId(firestoreId)?.let {
                    localRepository.updateTransaction(it.copy(photoUri = photoPath))
                }
            }
            else -> {}
        }
    }

    private fun handlePhotoUpdate(
        doc: DocumentSnapshot,
        currentPhotoUri: String?,
        firestoreId: String,
        type: SyncType
    ): String? {
        val storagePhotoUrl = doc.getString("photoStorageUrl")

        return when {
            storagePhotoUrl == null && currentPhotoUri != null -> null // Photo deleted
            !storagePhotoUrl.isNullOrBlank() && storagePhotoUrl != currentPhotoUri -> {
                downloadPhotoAsync(storagePhotoUrl, firestoreId, type)
                currentPhotoUri // Keep current until new downloads
            }
            else -> currentPhotoUri
        }
    }
    // --- NOTIFICATION ---
    private suspend fun sendPendingNotifications() {
        try {
            val token = messaging.token.await()
            Firebase.functions.getHttpsCallable("sendPendingNotifications")
                .call(hashMapOf("deviceToken" to token))
        } catch (e: Exception) {
        }
    }
    fun stopListening() {
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
    }
}

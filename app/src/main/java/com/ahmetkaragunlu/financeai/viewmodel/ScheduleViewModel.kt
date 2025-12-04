package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.photo.PhotoUploadWorker
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService,
    private val workManager: WorkManager,
) : ViewModel() {
    val scheduledTransactions: StateFlow<List<ScheduledTransactionEntity>> =
        repository.getAllScheduledTransactions()
            .distinctUntilChanged().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun executeScheduledTransaction(scheduledTx: ScheduledTransactionEntity) {
        viewModelScope.launch {
            val newFirestoreId = firebaseSyncService.getNewTransactionId()
            val newTransaction = TransactionEntity(
                id = 0,
                firestoreId = newFirestoreId,
                amount = scheduledTx.amount,
                transaction = scheduledTx.type,
                category = scheduledTx.category,
                note = scheduledTx.note ?: "",
                date = System.currentTimeMillis(),
                photoUri = scheduledTx.photoUri,
                locationFull = scheduledTx.locationFull,
                locationShort = scheduledTx.locationShort,
                latitude = scheduledTx.latitude,
                longitude = scheduledTx.longitude,
                syncedToFirebase = false
            )
            repository.insertTransaction(newTransaction)
            repository.deleteScheduledTransaction(scheduledTx)
            cancelNotificationWork(scheduledTx.id)
            syncChanges(newTransaction, scheduledTx)
            if (!newTransaction.photoUri.isNullOrBlank()) {
                enqueuePhotoUploadWorker(newTransaction)
            }
        }
    }

    private fun cancelNotificationWork(scheduledId: Long) {
        workManager.cancelAllWorkByTag("scheduled_notification_$scheduledId")
        workManager.cancelAllWorkByTag("delete_expired_$scheduledId")
    }

    private fun syncChanges(newTx: TransactionEntity, oldScheduledTx: ScheduledTransactionEntity) {
        viewModelScope.launch {
            try {
                if (newTx.firestoreId.isNotEmpty()) {
                    firebaseSyncService.syncTransactionToFirebase(newTx)
                }
                if (oldScheduledTx.firestoreId.isNotEmpty()) {
                    firebaseSyncService.deleteScheduledTransactionFromFirebase(oldScheduledTx.firestoreId)
                }
            } catch (e: Exception) {
            }
        }
    }
    private fun enqueuePhotoUploadWorker(transaction: TransactionEntity) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val uploadWork = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    PhotoUploadWorker.KEY_LOCAL_PATH to transaction.photoUri,
                    PhotoUploadWorker.KEY_FIRESTORE_ID to transaction.firestoreId,
                    PhotoUploadWorker.KEY_COLLECTION_TYPE to "transactions"
                )
            )
            .build()
        workManager.enqueue(uploadWork)
    }
}
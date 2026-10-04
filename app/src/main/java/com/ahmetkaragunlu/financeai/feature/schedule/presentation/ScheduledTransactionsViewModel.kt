package com.ahmetkaragunlu.financeai.feature.schedule.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.sync.TransactionSync
import com.ahmetkaragunlu.financeai.feature.schedule.domain.sync.ScheduledTransactionSync
import com.ahmetkaragunlu.financeai.photo.PhotoUploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduledTransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val transactionSync: TransactionSync,
    private val scheduledTransactionSync: ScheduledTransactionSync,
    private val workManager: WorkManager,
) : ViewModel() {
    val scheduledTransactions: StateFlow<List<ScheduledTransaction>> =
        scheduledTransactionRepository.observeScheduledTransactions()
            .distinctUntilChanged().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun executeScheduledTransaction(scheduledTx: ScheduledTransaction) {
        viewModelScope.launch {
            val newFirestoreId = transactionSync.createTransactionId()
            val newTransaction = Transaction(
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
            scheduledTransactionRepository.deleteScheduledTransaction(scheduledTx)
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

    private fun syncChanges(newTx: Transaction, oldScheduledTx: ScheduledTransaction) {
        viewModelScope.launch {
            try {
                if (newTx.firestoreId.isNotEmpty()) {
                    transactionSync.syncTransaction(newTx)
                }
                if (oldScheduledTx.firestoreId.isNotEmpty()) {
                    scheduledTransactionSync.deleteScheduledTransaction(oldScheduledTx.firestoreId)
                }
            } catch (e: Exception) {
            }
        }
    }
    private fun enqueuePhotoUploadWorker(transaction: Transaction) {
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

package com.ahmetkaragunlu.financeai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) : ViewModel() {


    val scheduledTransactions: StateFlow<List<ScheduledTransactionEntity>> =
        repo.getAllScheduledTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun confirmScheduledTransaction(
        scheduledTransaction: ScheduledTransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val transaction = TransactionEntity(
                    amount = scheduledTransaction.amount,
                    transaction = scheduledTransaction.type,
                    note = scheduledTransaction.note ?: "",
                    date = System.currentTimeMillis(),
                    category = scheduledTransaction.category
                )
                repo.insertTransaction(transaction)
                repo.deleteScheduledTransaction(scheduledTransaction)
                cancelPendingNotifications(scheduledTransaction.id)
                onSuccess()
            } catch (e: Exception) {
                onError(context.getString(R.string.error_transaction_confirm_failed, e.message ?: ""))
            }
        }
    }

    fun deleteScheduledTransaction(
        scheduledTransaction: ScheduledTransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.deleteScheduledTransaction(scheduledTransaction)
                cancelPendingNotifications(scheduledTransaction.id)
                onSuccess()
            } catch (e: Exception) {
                onError(context.getString(R.string.error_transaction_delete_failed, e.message ?: ""))
            }
        }
    }

    private fun cancelPendingNotifications(transactionId: Long) {
        workManager.cancelAllWorkByTag("scheduled_notification_$transactionId")
        workManager.cancelAllWorkByTag("delete_expired_$transactionId")
    }
}
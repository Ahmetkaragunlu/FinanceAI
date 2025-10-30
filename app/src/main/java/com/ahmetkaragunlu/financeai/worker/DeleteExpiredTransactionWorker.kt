package com.ahmetkaragunlu.financeai.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DeleteExpiredTransactionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: FinanceRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val transactionId = inputData.getLong(NotificationWorker.TRANSACTION_ID_KEY, -1L)
            if (transactionId == -1L) {
                return Result.failure()
            }
            val allTransactions = repository.getAllScheduledTransactions().first()
            val transaction = allTransactions.find { it.id == transactionId }

            transaction?.let {
                repository.deleteScheduledTransaction(it)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
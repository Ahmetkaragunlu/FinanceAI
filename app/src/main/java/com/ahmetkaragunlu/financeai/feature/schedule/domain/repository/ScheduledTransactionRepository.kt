package com.ahmetkaragunlu.financeai.feature.schedule.domain.repository

import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import kotlinx.coroutines.flow.Flow

interface ScheduledTransactionRepository {
    suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long
    suspend fun updateScheduledTransaction(transaction: ScheduledTransaction)
    suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction)
    fun observeScheduledTransactions(): Flow<List<ScheduledTransaction>>
    suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction?
    suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction?
}

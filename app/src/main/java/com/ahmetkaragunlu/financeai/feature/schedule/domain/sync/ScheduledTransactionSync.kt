package com.ahmetkaragunlu.financeai.feature.schedule.domain.sync

import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction

interface ScheduledTransactionSync {
    fun createScheduledTransactionId(): String
    suspend fun syncScheduledTransaction(transaction: ScheduledTransaction): Result<String>
    suspend fun deleteScheduledTransaction(firestoreId: String): Result<Unit>
}

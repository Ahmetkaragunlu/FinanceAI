package com.ahmetkaragunlu.financeai.feature.transaction.domain.sync

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

/** Existing remote operations only; a completed call is not a durable offline enqueue contract. */
interface TransactionSync {
    fun createTransactionId(): String
    suspend fun syncTransaction(transaction: Transaction): Result<String>
    suspend fun deleteTransaction(firestoreId: String): Result<Unit>
    suspend fun deleteTransactionPhoto(firestoreId: String): Result<Unit>
}

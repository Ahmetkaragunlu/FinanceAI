package com.ahmetkaragunlu.financeai.feature.transaction.testing

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.*
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Used by add/detail tests to control pending domain operations without a remote SDK. */
class RecordingTransactionRepository : TransactionRepository {
    val row = MutableStateFlow<Transaction?>(null)
    var observation: Flow<Transaction?> = row
    val insertions = mutableListOf<Transaction>()
    var onInsert: suspend (Transaction) -> Long = { 1L }
    var onDetails: suspend (Transaction, Double, String, CategoryType) -> Unit = { target, amount, note, category ->
        row.value = target.copy(amount = amount, note = note, category = category)
    }
    var onPhoto: suspend (Transaction, String?) -> String? = { target, path ->
        row.value = target.copy(photoUri = path)
        target.photoUri
    }
    var onDelete: suspend (Transaction) -> Unit = { row.value = null }
    override suspend fun insertTransaction(transaction: Transaction): Long {
        insertions += transaction
        return onInsert(transaction)
    }
    override suspend fun updateDetails(target: Transaction, amount: Double, note: String, category: CategoryType) = onDetails(target, amount, note, category)
    override suspend fun updatePhoto(target: Transaction, photoUri: String?): String? = onPhoto(target, photoUri)
    override suspend fun deleteTransaction(transaction: Transaction) = onDelete(transaction)
    override fun observeTransactionById(id: Int): Flow<Transaction?> = observation
    override fun observeTransactions(): Flow<List<Transaction>> = unexpected()
    override fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = unexpected()
    override fun observeTransactionsByCategoryAndDate(category: CategoryType, startDate: Long, endDate: Long): Flow<List<Transaction>> = unexpected()
    override fun observeFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> = unexpected()
    override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> = unexpected()
    override fun observeTransactionsByTypeAndDate(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<Transaction>> = unexpected()
    override fun observeCategoryExpensesByTypeAndDateRange(transactionType: TransactionType, startDate: Long, endDate: Long): Flow<List<CategoryExpense>> = unexpected()
    private fun unexpected(): Nothing = error("Unexpected transaction test query")
}

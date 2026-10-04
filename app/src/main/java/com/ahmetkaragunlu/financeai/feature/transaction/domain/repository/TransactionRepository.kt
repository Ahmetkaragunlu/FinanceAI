package com.ahmetkaragunlu.financeai.feature.transaction.domain.repository

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    fun observeTransactionById(id: Int): Flow<Transaction?>
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun observeTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>
    fun observeTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?>
    fun observeTransactionsByTypeAndDate(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>
    fun observeCategoryExpensesByTypeAndDateRange(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryExpense>>
    suspend fun getTransactionByFirestoreId(firestoreId: String): Transaction?
    fun observeUnsyncedTransactions(): Flow<List<Transaction>>
}

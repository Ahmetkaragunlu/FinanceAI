package com.ahmetkaragunlu.financeai.feature.transaction.domain.repository

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(transaction: Transaction)

    /** Only identity/ownership are read from the target; unrelated current fields are preserved. */
    suspend fun updateDetails(
        target: Transaction,
        amount: Double,
        note: String,
        category: CategoryType
    )

    /** Returns the previous photo path from the same atomic update, for guarded file cleanup. */
    suspend fun updatePhoto(target: Transaction, photoUri: String?): String?
    fun observeTransactionById(id: Int): Flow<Transaction?>
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun observeTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    fun observeFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary>
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
}

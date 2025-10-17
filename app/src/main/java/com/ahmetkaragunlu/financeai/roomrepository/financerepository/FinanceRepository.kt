package com.ahmetkaragunlu.financeai.roomrepository.financerepository

import com.ahmetkaragunlu.financeai.roomdb.model.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun updateTransaction(transaction: TransactionEntity)

    fun getAllTransactions(): Flow<List<TransactionEntity>>

    fun getAllTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    fun getTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>>

    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    fun getCategoryExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<CategoryExpense>>
}
package com.ahmetkaragunlu.financeai.roomrepository

import TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.model.TransactionEntity
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject



class TransactionRepo @Inject constructor(private val transactionDao: TransactionDao) {

    suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    fun getAllTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    fun getTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategoryAndDate(category, startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalIncomeByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalExpenseByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    fun getCategoryExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<CategoryExpense>> =
        transactionDao.getCategoryExpensesByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
}

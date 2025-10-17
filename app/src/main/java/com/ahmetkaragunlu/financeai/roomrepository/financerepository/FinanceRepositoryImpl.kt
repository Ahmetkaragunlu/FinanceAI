package com.ahmetkaragunlu.financeai.roomrepository.financerepository

import TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.model.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class FinanceRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : FinanceRepository {
    override suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getAllTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategoryAndDate(category, startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalIncomeByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalExpenseByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getCategoryExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<CategoryExpense>> =
        transactionDao.getCategoryExpensesByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
}

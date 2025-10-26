package com.ahmetkaragunlu.financeai.roomrepository.financerepository

import com.ahmetkaragunlu.financeai.roomdb.dao.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn


class FinanceRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val scheduledTransactionDao: ScheduledTransactionDao
) : FinanceRepository {


    //Transaction
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

    override fun getCategoryByTypeAndDateRange(
        transactionType: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryExpense>> =
        transactionDao.getCategoryByTypeAndDateRange(transactionType, startDate, endDate)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)




    //ScheduledTransaction
    override suspend fun insertScheduledTransaction(transaction: ScheduledTransactionEntity) =
        scheduledTransactionDao.insertScheduledTransaction(transaction)

    override suspend fun deleteScheduledTransaction(transaction: ScheduledTransactionEntity) =
        scheduledTransactionDao.deleteScheduledTransaction(transaction)

    override fun getAllScheduledTransactions(): Flow<List<ScheduledTransactionEntity>> =
        scheduledTransactionDao.getAllScheduledTransactions()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getPendingScheduledTransactions(currentTime: Long): Flow<List<ScheduledTransactionEntity>> =
        scheduledTransactionDao.getPendingScheduledTransactions(currentTime)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

}

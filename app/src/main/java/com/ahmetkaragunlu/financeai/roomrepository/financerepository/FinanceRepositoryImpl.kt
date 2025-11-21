package com.ahmetkaragunlu.financeai.roomrepository.financerepository

import com.ahmetkaragunlu.financeai.roomdb.dao.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.roomdb.dao.TransactionDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.ScheduledTransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class FinanceRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val scheduledTransactionDao: ScheduledTransactionDao
) : FinanceRepository {

    // Transaction
    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)


    override  suspend fun deleteTransactionByFirestoreId(firestoreId: String) {
        transactionDao.deleteTransactionByFirestoreId(firestoreId)
    }


    override fun getTransactionsByTypeAndDate(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByTypeAndDate(transactionType, startDate, endDate)
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

    override suspend fun getTransactionByFirestoreId(firestoreId: String): TransactionEntity? =
        transactionDao.getTransactionByFirestoreId(firestoreId)

    override fun getUnsyncedTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getUnsyncedTransactions()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    // Scheduled Transaction
    override suspend fun insertScheduledTransaction(transaction: ScheduledTransactionEntity): Long =
        scheduledTransactionDao.insertScheduledTransaction(transaction)

    override suspend fun updateScheduledTransaction(transaction: ScheduledTransactionEntity) =
        scheduledTransactionDao.updateScheduledTransaction(transaction)

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

    override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransactionEntity? =
        scheduledTransactionDao.getScheduledTransactionByFirestoreId(firestoreId)

    override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransactionEntity? =
        scheduledTransactionDao.getScheduledTransactionById(localId)

    override suspend fun deleteScheduledTransactionByFirestoreId(firestoreId: String) =
        scheduledTransactionDao.deleteByFirestoreId(firestoreId)

    override fun getUnsyncedScheduledTransactions(): Flow<List<ScheduledTransactionEntity>> =
        scheduledTransactionDao.getUnsyncedScheduledTransactions()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
}

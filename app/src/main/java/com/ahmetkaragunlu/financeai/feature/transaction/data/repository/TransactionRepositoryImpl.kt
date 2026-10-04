package com.ahmetkaragunlu.financeai.feature.transaction.data.repository

import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionRepository {
    override suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction.toEntity())

    override suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction.toEntity())

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeTransactions()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTransactionsByTypeAndDate(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        transactionDao.observeTransactionsByTypeAndDate(transactionType, startDate, endDate)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.observeTransactionsByDateRange(startDate, endDate)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        transactionDao.observeTransactionsByCategoryAndDate(category, startDate, endDate)
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.observeTotalIncomeByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.observeTotalExpenseByDateRange(startDate, endDate)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTransactionById(id: Int): Flow<Transaction?> =
        transactionDao.observeTransactionById(id)
            .map { it?.toDomain() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeCategoryExpensesByTypeAndDateRange(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryExpense>> =
        transactionDao.observeCategoryExpensesByTypeAndDateRange(transactionType, startDate, endDate)
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override suspend fun getTransactionByFirestoreId(firestoreId: String): Transaction? =
        transactionDao.getTransactionByFirestoreId(firestoreId)?.toDomain()

    override fun observeUnsyncedTransactions(): Flow<List<Transaction>> =
        transactionDao.observeUnsyncedTransactions()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
}

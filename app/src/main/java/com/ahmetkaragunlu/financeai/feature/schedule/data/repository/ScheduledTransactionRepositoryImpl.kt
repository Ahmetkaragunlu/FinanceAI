package com.ahmetkaragunlu.financeai.feature.schedule.data.repository

import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ScheduledTransactionRepositoryImpl @Inject constructor(
    private val scheduledTransactionDao: ScheduledTransactionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ScheduledTransactionRepository {
    override suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long =
        scheduledTransactionDao.insertScheduledTransaction(transaction.toEntity())

    override suspend fun updateScheduledTransaction(transaction: ScheduledTransaction) =
        scheduledTransactionDao.updateScheduledTransaction(transaction.toEntity())

    override suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction) =
        scheduledTransactionDao.deleteScheduledTransaction(transaction.toEntity())

    override fun observeScheduledTransactions(): Flow<List<ScheduledTransaction>> =
        scheduledTransactionDao.observeScheduledTransactions()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction? =
        scheduledTransactionDao.getScheduledTransactionByFirestoreId(firestoreId)?.toDomain()

    override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction? =
        scheduledTransactionDao.getScheduledTransactionById(localId)?.toDomain()

    override fun observeUnsyncedScheduledTransactions(): Flow<List<ScheduledTransaction>> =
        scheduledTransactionDao.observeUnsyncedScheduledTransactions()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
}

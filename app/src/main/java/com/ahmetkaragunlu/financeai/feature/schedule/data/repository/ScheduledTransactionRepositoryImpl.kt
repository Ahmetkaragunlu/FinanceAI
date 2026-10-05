package com.ahmetkaragunlu.financeai.feature.schedule.data.repository

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.toFirebaseMap
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ScheduledTransactionRepositoryImpl @Inject constructor(
    private val scheduledTransactionDao: ScheduledTransactionDao,
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pendingChanges: PendingChanges,
    private val scheduler: SyncScheduler
) : ScheduledTransactionRepository {
    override suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long = save(transaction)

    private suspend fun save(value: ScheduledTransaction): Long = session.withAccount { account ->
        require(value.ownerId.isEmpty() || value.ownerId == account.ownerId)
        require(value.currencyCode == "XXX" || value.currencyCode == account.currencyCode)
        require(MoneyAmounts.toMinor(value.amount, account.currencyCode) > 0)
        val prepared = value.copy(ownerId = account.ownerId, currencyCode = account.currencyCode,
            firestoreId = value.firestoreId.ifBlank { UUID.randomUUID().toString() }, syncedToFirebase = false)
        val id = database.withTransaction {
            val existing = scheduledTransactionDao.getScheduledTransactionByFirestoreId(prepared.firestoreId)
            check(prepared.id == 0L || existing?.id == prepared.id) { "Stale record" }
            val row = prepared.toEntity().copy(id = existing?.id ?: 0L)
            val result = scheduledTransactionDao.insertScheduledTransaction(row)
            pendingChanges.record(account.ownerId, "scheduled_transactions", row.firestoreId, prepared.toFirebaseMap())
            result
        }
        scheduler.enqueue(account.ownerId)
        id
    }

    override suspend fun updateScheduledTransaction(transaction: ScheduledTransaction) { save(transaction) }

    override suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction) {
        session.withAccount { account ->
            require(transaction.ownerId.isEmpty() || transaction.ownerId == account.ownerId)
            database.withTransaction {
                val existing = scheduledTransactionDao.getScheduledTransactionByFirestoreId(transaction.firestoreId) ?: return@withTransaction
                scheduledTransactionDao.deleteScheduledTransaction(existing)
                pendingChanges.record(account.ownerId, "scheduled_transactions", existing.firestoreId, null)
            }
            scheduler.enqueue(account.ownerId)
        }
    }

    override fun observeScheduledTransactions(): Flow<List<ScheduledTransaction>> =
        session.observe<List<ScheduledTransaction>>(emptyList<ScheduledTransaction>()) { account ->
            scheduledTransactionDao.observeScheduledTransactions()
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction? =
        scheduledTransactionDao.getScheduledTransactionByFirestoreId(firestoreId)?.toDomain()

    override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction? =
        scheduledTransactionDao.getScheduledTransactionById(localId)?.toDomain()

}

package com.ahmetkaragunlu.financeai.feature.schedule.data.repository

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.money.UNSPECIFIED_CURRENCY
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.core.sync.local.PendingChanges
import com.ahmetkaragunlu.financeai.feature.schedule.data.local.dao.ScheduledTransactionDao
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.schedule.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderScheduler
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.toFirebaseMap
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.reminder.ReminderPresenter
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import java.io.File
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
    private val scheduler: SyncScheduler,
    private val reminders: ReminderScheduler,
    private val presenter: ReminderPresenter
) : ScheduledTransactionRepository {
    override suspend fun insertScheduledTransaction(transaction: ScheduledTransaction): Long =
        save(transaction)

    private suspend fun save(value: ScheduledTransaction): Long = session.withAccount { account ->
        require(value.ownerId.isEmpty() || value.ownerId == account.ownerId)
        require(value.currencyCode == UNSPECIFIED_CURRENCY || value.currencyCode == account.currencyCode)
        require(MoneyAmounts.toMinor(value.amount, account.currencyCode) > 0)
        val prepared = value.copy(
            ownerId = account.ownerId,
            currencyCode = account.currencyCode,
            firestoreId = value.firestoreId.ifBlank { UUID.randomUUID().toString() },
            syncedToFirebase = false
        )
        val id = database.withTransaction {
            val existing =
                scheduledTransactionDao.getScheduledTransactionByFirestoreId(prepared.firestoreId)
            if (prepared.id != 0L && existing?.id != prepared.id) throw DataAccessException.StaleRecord()
            val row = prepared.toEntity().copy(id = existing?.id ?: 0L)
            val result = scheduledTransactionDao.insertScheduledTransaction(row)
            val payload = prepared.toFirebaseMap().toMutableMap()
            if (row.photoUri != null && row.photoUri != existing?.photoUri && !row.photoUri.startsWith(
                    "http"
                )
            )
                payload[PhotoFields.INTENT] = File(row.photoUri).nameWithoutExtension
            pendingChanges.record(
                account.ownerId,
                FirestoreCollections.SCHEDULED_TRANSACTIONS,
                row.firestoreId,
                payload
            )
            result
        }
        scheduler.enqueue(account.ownerId)
        reminders.wake(account.ownerId, prepared.firestoreId)
        id
    }


    override suspend fun deleteScheduledTransaction(transaction: ScheduledTransaction) {
        session.withAccount { account ->
            require(transaction.ownerId.isEmpty() || transaction.ownerId == account.ownerId)
            database.withTransaction {
                val existing =
                    scheduledTransactionDao.getScheduledTransactionByFirestoreId(transaction.firestoreId)
                        ?: return@withTransaction
                scheduledTransactionDao.deleteScheduledTransaction(existing)
                pendingChanges.record(
                    account.ownerId,
                    FirestoreCollections.SCHEDULED_TRANSACTIONS,
                    existing.firestoreId,
                    null
                )
                reminders.cancel(account.ownerId, existing.firestoreId, existing.id)
                presenter.cancel(account.ownerId, existing.firestoreId)
            }
            scheduler.enqueue(account.ownerId)
        }
    }

    override fun observeScheduledTransactions(): Flow<List<ScheduledTransaction>> =
        session.observe(emptyList()) { _ ->
            scheduledTransactionDao.observeScheduledTransactions()
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override suspend fun getScheduledTransactionByFirestoreId(firestoreId: String): ScheduledTransaction? =
        scheduledTransactionDao.getScheduledTransactionByFirestoreId(firestoreId)?.toDomain()

    override suspend fun getScheduledTransactionById(localId: Long): ScheduledTransaction? =
        scheduledTransactionDao.getScheduledTransactionById(localId)?.toDomain()

}

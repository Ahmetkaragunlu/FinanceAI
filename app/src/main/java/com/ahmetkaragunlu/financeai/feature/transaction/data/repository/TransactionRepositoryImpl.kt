package com.ahmetkaragunlu.financeai.feature.transaction.data.repository

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.local.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.dao.TransactionDao
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.entity.TransactionEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.transaction.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.toFirebaseMap
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import java.util.UUID
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pendingChanges: PendingChanges,
    private val scheduler: SyncScheduler
) : TransactionRepository {
    override suspend fun insertTransaction(transaction: Transaction): Long = save(transaction)

    private suspend fun save(value: Transaction): Long = session.withAccount { account ->
        require(value.ownerId.isEmpty() || value.ownerId == account.ownerId)
        require(value.currencyCode == "XXX" || value.currencyCode == account.currencyCode)
        require(MoneyAmounts.toMinor(value.amount, account.currencyCode) > 0)
        val prepared = value.copy(ownerId = account.ownerId, currencyCode = account.currencyCode,
            firestoreId = value.firestoreId.ifBlank { UUID.randomUUID().toString() }, syncedToFirebase = false)
        val id = database.withTransaction {
            val existing = transactionDao.getTransactionByFirestoreId(prepared.firestoreId)
            if (prepared.id != 0 && existing?.id != prepared.id) throw DataAccessException.StaleRecord()
            persist(prepared, existing)
        }
        scheduler.enqueue(account.ownerId)
        id
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        session.withAccount { account ->
            require(transaction.ownerId.isEmpty() || transaction.ownerId == account.ownerId)
            database.withTransaction {
                val existing = transactionDao.getTransactionByFirestoreId(transaction.firestoreId) ?: return@withTransaction
                transactionDao.deleteTransaction(existing)
                pendingChanges.record(account.ownerId, FirestoreCollections.TRANSACTIONS, existing.firestoreId, null)
            }
            scheduler.enqueue(account.ownerId)
        }
    }

    override suspend fun updateDetails(target: Transaction, amount: Double, note: String, category: CategoryType) {
        mutate(target) { current -> current.copy(amount = amount, note = note, category = category) }
    }

    override suspend fun updatePhoto(target: Transaction, photoUri: String?): String? =
        mutate(target) { current -> current.copy(photoUri = photoUri) }.photoUri

    private suspend fun mutate(target: Transaction, change: (Transaction) -> Transaction): Transaction =
        session.withAccount { account ->
            require(target.ownerId == account.ownerId)
            require(target.currencyCode == account.currencyCode)
            val previous = database.withTransaction {
                val existing = transactionDao.getTransactionByFirestoreId(target.firestoreId)
                    ?: throw DataAccessException.StaleRecord()
                if (existing.id != target.id) throw DataAccessException.StaleRecord()
                val current = existing.toDomain()
                val updated = change(current).copy(syncedToFirebase = false)
                require(MoneyAmounts.toMinor(updated.amount, account.currencyCode) > 0)
                persist(updated, existing)
                current
            }
            scheduler.enqueue(account.ownerId)
            previous
        }

    /** Caller owns the Room transaction; the row and its durable sync intention cannot diverge. */
    private suspend fun persist(value: Transaction, existing: TransactionEntity?): Long {
        val row = value.toEntity().copy(id = existing?.id ?: 0)
        val result = transactionDao.insertTransaction(row)
        val payload = value.toFirebaseMap().toMutableMap()
        if (existing?.photoUri != null && row.photoUri == null) {
            payload[PhotoFields.STORAGE_URL] = null
            payload[PhotoFields.REMOVED] = true
        } else if (row.photoUri != null && row.photoUri != existing?.photoUri) {
            payload[PhotoFields.REMOVED] = false
            if (!row.photoUri.startsWith("http")) payload[PhotoFields.INTENT] = File(row.photoUri).nameWithoutExtension
        }
        pendingChanges.record(value.ownerId, FirestoreCollections.TRANSACTIONS, row.firestoreId, payload)
        return result
    }

    override fun observeTransactions(): Flow<List<Transaction>> =
        session.observe<List<Transaction>>(emptyList<Transaction>()) { account ->
            transactionDao.observeTransactions()
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override fun observeTransactionsByTypeAndDate(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        session.observe<List<Transaction>>(emptyList<Transaction>()) { account ->
            transactionDao.observeTransactionsByTypeAndDate(transactionType, startDate, endDate)
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override fun observeTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        session.observe<List<Transaction>>(emptyList<Transaction>()) { account ->
            transactionDao.observeTransactionsByDateRange(startDate, endDate)
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override fun observeTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        session.observe<List<Transaction>>(emptyList<Transaction>()) { account ->
            transactionDao.observeTransactionsByCategoryAndDate(category, startDate, endDate)
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override fun observeFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> =
        session.observe(FinancialSummary()) { account ->
            transactionDao.observeFinancialSummary(startDate, endDate).map { row ->
                FinancialSummary(
                    MoneyAmounts.toMajor(row.incomeMinor, account.currencyCode),
                    MoneyAmounts.toMajor(row.expenseMinor, account.currencyCode))
            }.distinctUntilChanged()
        }

    override fun observeTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Double?> =
        session.observe<Double?>(null) { account ->
            transactionDao.observeTotalExpenseByDateRange(startDate, endDate)
                .map { it?.let { total -> MoneyAmounts.toMajor(total, account.currencyCode) } }
                .distinctUntilChanged()
        }

    override fun observeTransactionById(id: Int): Flow<Transaction?> =
        session.observe<Transaction?>(null) { account ->
            transactionDao.observeTransactionById(id)
                .map { it?.toDomain() }
                .distinctUntilChanged()
        }

    override fun observeCategoryExpensesByTypeAndDateRange(
        transactionType: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryExpense>> =
        session.observe<List<CategoryExpense>>(emptyList<CategoryExpense>()) { account ->
            transactionDao.observeCategoryExpensesByTypeAndDateRange(transactionType, startDate, endDate)
                .map { rows -> rows.map { CategoryExpense(it.category, MoneyAmounts.toMajor(it.totalMinor, account.currencyCode)) } }
                .distinctUntilChanged()
        }

}

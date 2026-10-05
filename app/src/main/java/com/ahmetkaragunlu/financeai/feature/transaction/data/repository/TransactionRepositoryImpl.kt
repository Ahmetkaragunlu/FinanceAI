package com.ahmetkaragunlu.financeai.feature.transaction.data.repository

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.transaction.data.local.TransactionDao
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
            check(prepared.id == 0 || existing?.id == prepared.id) { "Stale record" }
            val row = prepared.toEntity().copy(id = existing?.id ?: 0)
            val result = transactionDao.insertTransaction(row)
            val payload = prepared.toFirebaseMap().toMutableMap()
            if (existing?.photoUri != null && row.photoUri == null) {
                payload["photoStorageUrl"] = null
                payload["photoRemoved"] = true
            } else if (row.photoUri != null && row.photoUri != existing?.photoUri) payload["photoRemoved"] = false
            pendingChanges.record(account.ownerId, "transactions", row.firestoreId, payload)
            result
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
                pendingChanges.record(account.ownerId, "transactions", existing.firestoreId, null)
            }
            scheduler.enqueue(account.ownerId)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) { save(transaction) }

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

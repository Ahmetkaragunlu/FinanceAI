package com.ahmetkaragunlu.financeai.feature.budget.data.repository

import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections

import androidx.room.withTransaction
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.sync.PendingChanges
import com.ahmetkaragunlu.financeai.core.sync.SyncScheduler
import com.ahmetkaragunlu.financeai.feature.budget.data.local.dao.BudgetDao
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.budget.data.remote.toFirebaseMap
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val database: FinanceDatabase,
    private val session: AccountSession,
    private val pendingChanges: PendingChanges,
    private val scheduler: SyncScheduler
) : BudgetRepository {

    override suspend fun insertBudget(budget: Budget): Long = save(budget)

    private suspend fun save(value: Budget): Long = session.withAccount { account ->
        require(value.ownerId.isEmpty() || value.ownerId == account.ownerId)
        require(value.currencyCode == "XXX" || value.currencyCode == account.currencyCode)
        if (value.budgetType == BudgetType.CATEGORY_PERCENTAGE) {
            require(value.limitPercentage?.let { it.isFinite() && it > 0 } == true)
        } else require(MoneyAmounts.toMinor(value.amount, account.currencyCode) > 0)
        val prepared = value.copy(ownerId = account.ownerId, currencyCode = account.currencyCode,
            firestoreId = value.firestoreId.ifBlank { account.ownerId + "_budget_" + (if (value.budgetType == BudgetType.GENERAL_MONTHLY) "general" else requireNotNull(value.category).name) }, syncedToFirebase = false)
        val id = database.withTransaction {
            val duplicate = budgetDao.getAllBudgetsOneShot().firstOrNull { row ->
                row.id != prepared.id && (if (prepared.budgetType == BudgetType.GENERAL_MONTHLY) row.budgetType == prepared.budgetType else row.category == prepared.category)
            }
            if (duplicate != null) throw BudgetException.DuplicateRule()
            val existing = budgetDao.getBudgetByFirestoreId(prepared.firestoreId)
            if (prepared.id != 0 && existing?.id != prepared.id) throw DataAccessException.StaleRecord()
            val row = prepared.toEntity().copy(id = existing?.id ?: 0)
            val result = budgetDao.insertBudget(row)
            pendingChanges.record(account.ownerId, FirestoreCollections.BUDGETS, row.firestoreId, prepared.toFirebaseMap())
            result
        }
        scheduler.enqueue(account.ownerId)
        id
    }

    override suspend fun updateBudget(budget: Budget) { save(budget) }

    override suspend fun deleteBudget(budget: Budget) {
        session.withAccount { account ->
            require(budget.ownerId.isEmpty() || budget.ownerId == account.ownerId)
            database.withTransaction {
                val existing = budgetDao.getBudgetByFirestoreId(budget.firestoreId) ?: return@withTransaction
                budgetDao.deleteBudget(existing)
                pendingChanges.record(account.ownerId, FirestoreCollections.BUDGETS, existing.firestoreId, null)
            }
            scheduler.enqueue(account.ownerId)
        }
    }

    override fun observeBudgets(): Flow<List<Budget>> =
        session.observe<List<Budget>>(emptyList<Budget>()) { account ->
            budgetDao.observeBudgets()
                .map { rows -> rows.map { it.toDomain() } }
                .distinctUntilChanged()
        }

    override fun observeGeneralBudget(): Flow<Budget?> =
        session.observe<Budget?>(null) { account ->
            budgetDao.observeGeneralBudget()
                .map { it?.toDomain() }
                .distinctUntilChanged()
        }

    override suspend fun getBudgetByCategory(category: CategoryType): Budget? =
        budgetDao.getBudgetByCategory(category)?.toDomain()

    override suspend fun getAllBudgetsOneShot(): List<Budget>  =
        budgetDao.getAllBudgetsOneShot().map { it.toDomain() }

}

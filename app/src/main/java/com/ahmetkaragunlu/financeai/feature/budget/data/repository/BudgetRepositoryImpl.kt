package com.ahmetkaragunlu.financeai.feature.budget.data.repository

import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetDao
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.toDomain
import com.ahmetkaragunlu.financeai.feature.budget.data.mapper.toEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BudgetRepository {

    override suspend fun insertBudget(budget: Budget): Long =
        budgetDao.insertBudget(budget.toEntity())

    override suspend fun updateBudget(budget: Budget) =
        budgetDao.updateBudget(budget.toEntity())

    override suspend fun deleteBudget(budget: Budget) =
        budgetDao.deleteBudget(budget.toEntity())

    override fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeBudgets()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeGeneralBudget(): Flow<Budget?> =
        budgetDao.observeGeneralBudget()
            .map { it?.toDomain() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override suspend fun getBudgetByCategory(category: CategoryType): Budget? =
        budgetDao.getBudgetByCategory(category)?.toDomain()

    override fun observeUnsyncedBudgets(): Flow<List<Budget>> =
        budgetDao.observeUnsyncedBudgets()
            .map { rows -> rows.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override suspend fun getBudgetByFirestoreId(firestoreId: String): Budget? =
        budgetDao.getBudgetByFirestoreId(firestoreId)?.toDomain()

    override suspend fun getAllBudgetsOneShot(): List<Budget>  =
        budgetDao.getAllBudgetsOneShot().map { it.toDomain() }

}

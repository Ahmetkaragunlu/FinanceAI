package com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy

import com.ahmetkaragunlu.financeai.roomdb.dao.BudgetDao
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override suspend fun insertBudget(budget: BudgetEntity): Long =
        budgetDao.insertBudget(budget)

    override suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override fun getGeneralBudget(): Flow<BudgetEntity?> =
        budgetDao.getGeneralBudget()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    override suspend fun getBudgetByCategory(category: CategoryType): BudgetEntity? =
        budgetDao.getBudgetByCategory(category)

    override fun getUnsyncedBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getUnsyncedBudgets()
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
}
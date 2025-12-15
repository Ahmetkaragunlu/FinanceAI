package com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy

import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    fun getGeneralBudget(): Flow<BudgetEntity?>
    suspend fun getBudgetByCategory(category: CategoryType): BudgetEntity?
    fun getUnsyncedBudgets(): Flow<List<BudgetEntity>>
    suspend fun getBudgetByFirestoreId(firestoreId: String): BudgetEntity?
    suspend fun getAllBudgetsOneShot(): List<BudgetEntity>
}
package com.ahmetkaragunlu.financeai.feature.budget.domain.repository

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun insertBudget(budget: Budget): Long
    suspend fun deleteBudget(budget: Budget)
    fun observeBudgets(): Flow<List<Budget>>
    fun observeGeneralBudget(): Flow<Budget?>
    suspend fun getBudgetByCategory(category: CategoryType): Budget?
    suspend fun getAllBudgetsOneShot(): List<Budget>
}

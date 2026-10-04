package com.ahmetkaragunlu.financeai.feature.budget.domain.sync

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget

interface BudgetSync {
    fun createBudgetId(): String
    suspend fun syncBudget(budget: Budget): Result<String>
    suspend fun deleteBudget(firestoreId: String): Result<Unit>
}

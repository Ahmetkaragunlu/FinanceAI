package com.ahmetkaragunlu.financeai.viewmodel

import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType

data class BudgetUiState(
    val generalBudgetState: GeneralBudgetState? = null,
    val categoryBudgetStates: List<CategoryBudgetState> = emptyList(),
    val aiWarningMessage: String? = null,
    val isBudgetEmpty: Boolean = false
)

data class GeneralBudgetState(
    val id: Int,
    val limitAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progress: Float,
    val incomeAmount: Double,
    val expenseAmount: Double
)

data class CategoryBudgetState(
    val id: Int,
    val category: CategoryType,
    val budgetType: BudgetType,
    val limitAmount: Double,
    val spentAmount: Double,
    val limitPercentage: Double? = null,
    val progress: Float,
    val isOverBudget: Boolean,
    val percentageUsed: Int
)
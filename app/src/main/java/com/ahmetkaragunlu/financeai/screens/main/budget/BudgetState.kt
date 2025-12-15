package com.ahmetkaragunlu.financeai.screens.main.budget

import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import androidx.annotation.StringRes


data class BudgetUiState(
    val generalBudgetState: GeneralBudgetState? = null,
    val categoryBudgetStates: List<CategoryBudgetState> = emptyList(),
    @StringRes val warningMessageResId: Int? = null,
    val warningMessageArgs: List<Any> = emptyList(),
    val isBudgetEmpty: Boolean = false,
    val isLoading: Boolean = false
)

data class BudgetFormState(
    val isVisible: Boolean = false,
    val editingId: Int = 0,
    val selectedType: BudgetType = BudgetType.CATEGORY_AMOUNT,
    val amountInput: String = "",
    val percentageInput: String = "",
    val selectedCategory: CategoryType? = null,
    val isConflictDialogOpen: Boolean = false,
    @StringRes val conflictErrorResId: Int? = null,
    @StringRes val amountErrorResId: Int? = null,
    @StringRes val categoryErrorResId: Int? = null
)

data class DeleteDialogState(
    val isVisible: Boolean = false,
    val budgetIdToDelete: Int? = null
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

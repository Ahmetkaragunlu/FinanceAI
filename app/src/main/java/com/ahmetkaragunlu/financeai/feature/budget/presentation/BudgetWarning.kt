package com.ahmetkaragunlu.financeai.feature.budget.presentation

import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

sealed interface BudgetWarning {
    data object GeneralExceeded : BudgetWarning
    data object GeneralNearLimit : BudgetWarning
    data class CategoryExceeded(val category: CategoryType) : BudgetWarning
    data class CategoriesExceeded(val count: Int) : BudgetWarning
    data class GeneralAndCategoryExceeded(val category: CategoryType) : BudgetWarning
    data class GeneralAndCategoriesExceeded(val count: Int) : BudgetWarning
}

internal fun budgetWarning(categories: List<CategoryBudgetState>, general: GeneralBudgetState?): BudgetWarning? {
    val generalOver = general != null && general.remainingAmount < 0
    val over = categories.filter { it.isOverBudget }
    return when {
        generalOver && over.size == 1 -> BudgetWarning.GeneralAndCategoryExceeded(over.single().category)
        generalOver && over.size > 1 -> BudgetWarning.GeneralAndCategoriesExceeded(over.size)
        generalOver -> BudgetWarning.GeneralExceeded
        over.size == 1 -> BudgetWarning.CategoryExceeded(over.single().category)
        over.size > 1 -> BudgetWarning.CategoriesExceeded(over.size)
        general != null && general.progress > 0.85f -> BudgetWarning.GeneralNearLimit
        else -> null
    }
}

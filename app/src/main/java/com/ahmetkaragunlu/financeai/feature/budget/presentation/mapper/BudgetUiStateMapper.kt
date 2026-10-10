package com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper

import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateBudgetUsagePercentage
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateCategoryBudgetLimit
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetUiState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.GeneralBudgetState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.CategoryBudgetState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.budgetWarning
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

internal fun mapBudgetUiState(
    rules: List<Budget>,
    totalIncome: Double,
    totalExpense: Double,
    categoryExpenses: List<CategoryExpense>
): BudgetUiState {
    val generalRule = rules.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
    val generalBudgetState = generalRule?.let { rule ->
        val limit = rule.amount
        val progress = if (limit > 0) (totalExpense / limit).toFloat() else 0f
        GeneralBudgetState(
            id = rule.id,
            limitAmount = limit,
            spentAmount = totalExpense,
            remainingAmount = limit - totalExpense,
            progress = progress,
            incomeAmount = totalIncome,
            expenseAmount = totalExpense
        )
    }
    val categoryBudgetStates = rules.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }
        .map { rule ->
            val spent = rule.category?.let { category ->
                categoryExpenses.find { it.category == category }?.totalAmount
            } ?: 0.0
            val limit = calculateCategoryBudgetLimit(rule, generalRule)
            val progress = if (limit > 0) (spent / limit).toFloat() else 0f
            CategoryBudgetState(
                id = rule.id,
                category = rule.category ?: CategoryType.OTHER,
                budgetType = rule.budgetType,
                limitAmount = limit,
                limitPercentage = rule.limitPercentage,
                spentAmount = spent,
                progress = progress.coerceIn(0f, 1f),
                isOverBudget = spent > limit,
                percentageUsed = calculateBudgetUsagePercentage(spent, limit).toInt()
            )
        }.sortedByDescending { it.percentageUsed }
    return BudgetUiState(
        isBudgetEmpty = false,
        generalBudgetState = generalBudgetState,
        categoryBudgetStates = categoryBudgetStates,
        warning = budgetWarning(categoryBudgetStates, generalBudgetState)
    )
}

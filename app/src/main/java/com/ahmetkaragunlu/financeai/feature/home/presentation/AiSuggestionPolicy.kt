package com.ahmetkaragunlu.financeai.feature.home.presentation

import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateBudgetUsagePercentage
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateCategoryBudgetLimit
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense

private const val BUDGET_WARNING_THRESHOLD = 80.0

/** Preserve the existing priority: general warning, first category warning, healthy budget. */
fun buildAiSuggestion(
    budgets: List<Budget>,
    totalExpense: Double,
    expenses: List<CategoryExpense>,
): AiSuggestionState {
    if (budgets.isEmpty())
        return if (totalExpense > 0) AiSuggestionState.NoBudget(totalExpense)
        else AiSuggestionState.Planning
    val general = budgets.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
    if (general != null) {
        val usage = calculateBudgetUsagePercentage(totalExpense, general.amount)
        if (totalExpense > general.amount)
            return AiSuggestionState.GeneralExceeded(general.amount, totalExpense, usage.toInt())
        if (usage >= BUDGET_WARNING_THRESHOLD)
            return AiSuggestionState.GeneralNearLimit(usage.toInt())
    }
    for (budget in budgets.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }) {
        val category = budget.category ?: continue
        val spent = expenses.find { it.category == category.name }?.totalAmount ?: 0.0
        val limit = calculateCategoryBudgetLimit(budget, general)
        val usage = calculateBudgetUsagePercentage(spent, limit)
        if (spent > limit) return AiSuggestionState.CategoryExceeded(category, limit, spent)
        if (usage >= BUDGET_WARNING_THRESHOLD)
            return AiSuggestionState.CategoryNearLimit(category, usage.toInt())
    }
    return AiSuggestionState.Healthy
}

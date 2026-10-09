package com.ahmetkaragunlu.financeai.feature.home.presentation

import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.BudgetWarningThreshold
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateBudgetUsagePercentage
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateCategoryBudgetLimit
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense

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
        if (usage >= BudgetWarningThreshold.PERCENTAGE)
            return AiSuggestionState.GeneralNearLimit(usage.toInt())
    }
    for (budget in budgets.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }) {
        val category = budget.category ?: continue
        val spent = expenses.find { it.category == category }?.totalAmount ?: 0.0
        val limit = calculateCategoryBudgetLimit(budget, general)
        val usage = calculateBudgetUsagePercentage(spent, limit)
        if (spent > limit) return AiSuggestionState.CategoryExceeded(category, limit, spent)
        if (usage >= BudgetWarningThreshold.PERCENTAGE)
            return AiSuggestionState.CategoryNearLimit(category, usage.toInt())
    }
    return AiSuggestionState.Healthy
}

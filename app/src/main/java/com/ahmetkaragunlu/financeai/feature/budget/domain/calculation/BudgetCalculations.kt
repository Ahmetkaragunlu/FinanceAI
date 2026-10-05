package com.ahmetkaragunlu.financeai.feature.budget.domain.calculation

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType

fun calculateCategoryBudgetLimit(budget: Budget, generalBudget: Budget?): Double =
    if (budget.budgetType == BudgetType.CATEGORY_PERCENTAGE && generalBudget != null) {
        generalBudget.amount * ((budget.limitPercentage ?: 0.0) / 100)
    } else {
        budget.amount
    }

fun calculateBudgetUsagePercentage(spent: Double, limit: Double): Double =
    if (limit > 0) (spent / limit) * 100 else 0.0

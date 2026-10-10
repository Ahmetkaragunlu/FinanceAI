package com.ahmetkaragunlu.financeai.feature.aichat.domain.report

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

data class BudgetUsage(
    val budget: Budget,
    val limit: Double,
    val spent: Double,
    val percentage: Double
)

data class CategorySpending(
    val category: CategoryType,
    val amount: Double,
    val percentage: Double
)

data class FinancialReport(
    val summary: FinancialSummary,
    val budgetUsage: List<BudgetUsage>,
    val transactions: List<Transaction>,
    val categorySpending: List<CategorySpending>,
    val topSpendingCategories: List<CategorySpending>
)

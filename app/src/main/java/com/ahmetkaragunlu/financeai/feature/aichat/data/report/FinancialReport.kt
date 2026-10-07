package com.ahmetkaragunlu.financeai.feature.aichat.data.report

import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateBudgetUsagePercentage
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateCategoryBudgetLimit
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

data class BudgetUsage(val budget: Budget, val limit: Double, val spent: Double, val percentage: Double)
data class CategorySpending(val category: CategoryType, val amount: Double, val percentage: Double)
data class FinancialReport(
    val summary: FinancialSummary,
    val budgetUsage: List<BudgetUsage>,
    val transactions: List<Transaction>,
    val categorySpending: List<CategorySpending>,
    val topSpendingCategories: List<CategorySpending>
)

/** Every total, ranking and budget usage shares the same calendar-month range. */
fun FinancialSnapshot.calculateReport(): FinancialReport {
    require(monthStart < monthEndExclusive)
    val monthlyTransactions = transactions.filter { it.date >= monthStart && it.date < monthEndExclusive }
    fun total(type: TransactionType) = MoneyAmounts.sum(
        monthlyTransactions.filter { it.transaction == type }.map { it.amount }, currencyCode)
    val summary = FinancialSummary(total(TransactionType.INCOME), total(TransactionType.EXPENSE))
    val monthlyExpenses = monthlyTransactions.filter { it.transaction == TransactionType.EXPENSE }
    val categories = monthlyExpenses.groupBy { it.category }.map { (category, rows) ->
        val amount = MoneyAmounts.sum(rows.map { it.amount }, currencyCode)
        CategorySpending(category, amount, calculateBudgetUsagePercentage(amount, summary.expense))
    }.sortedWith(compareByDescending<CategorySpending> { it.amount }.thenBy { it.category.name })
    val highestExpense = categories.firstOrNull()?.amount
    val topCategories = categories.filter { it.amount == highestExpense }
    val general = budgets.firstOrNull { it.category == null }
    val usages = budgets.map { budget ->
        val spent = MoneyAmounts.sum(monthlyExpenses.filter {
            budget.category == null || it.category == budget.category
        }.map { it.amount }, currencyCode)
        val limit = if (budget.category == null) budget.amount else calculateCategoryBudgetLimit(budget, general)
        BudgetUsage(budget, limit, spent, calculateBudgetUsagePercentage(spent, limit))
    }
    return FinancialReport(summary, usages, monthlyTransactions, categories, topCategories)
}

package com.ahmetkaragunlu.financeai.feature.aichat.data.report

import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class FinancialReportTest {
    private fun expense(amount: Double, at: Long) = Transaction(amount = amount, date = at,
        transaction = TransactionType.EXPENSE, category = CategoryType.FOOD, currencyCode = "USD")

    @Test fun summaryTransactionsAndBudgetUsageAllUseTheSameMonthBoundaries() {
        val snapshot = FinancialSnapshot("USD", 150, 100, 200,
            listOf(expense(0.10, 99), expense(0.20, 100), expense(0.30, 199), expense(0.40, 200)),
            listOf(Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 10.0),
                Budget(budgetType = BudgetType.CATEGORY_PERCENTAGE, category = CategoryType.FOOD, limitPercentage = 25.0)))
        val report = snapshot.calculateReport()
        assertEquals(0.5, report.summary.expense, 0.0)
        assertEquals(-0.5, report.summary.remainingBalance, 0.0)
        assertEquals(listOf(100L, 199L), report.transactions.map { it.date })
        assertEquals(0.5, report.budgetUsage[0].spent, 0.0)
        assertEquals(2.5, report.budgetUsage[1].limit, 0.0)
        assertEquals(20.0, report.budgetUsage[1].percentage, 0.0)
        assertEquals(0.5, report.categorySpending.single().amount, 0.0)
    }

    @Test fun budgetOnlyAccountStillProducesEveryBudgetLimit() {
        val snapshot = FinancialSnapshot("USD", 1, 0, 2, emptyList(), listOf(
            Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 500.0),
            Budget(budgetType = BudgetType.CATEGORY_AMOUNT, category = CategoryType.FOOD, amount = 75.0)))
        val report = snapshot.calculateReport()
        assertEquals(2, report.budgetUsage.size)
        assertEquals(listOf(500.0, 75.0), report.budgetUsage.map { it.limit })
        assertTrue(report.budgetUsage.all { it.spent == 0.0 && it.percentage == 0.0 })
        assertTrue(report.transactions.isEmpty())
        assertTrue(report.topSpendingCategories.isEmpty())
    }

    @Test fun categoryRankingUsesMonthlyExpensesAndExcludesIncomeAndOldRecords() {
        val snapshot = FinancialSnapshot("USD", 150, 100, 200, listOf(
            expense(50.0, 120), expense(50.0, 130),
            expense(40.0, 140).copy(category = CategoryType.TRANSPORT),
            expense(60.0, 150).copy(category = CategoryType.CLOTHING),
            expense(999.0, 99).copy(category = CategoryType.TRANSPORT),
            expense(500.0, 150).copy(transaction = TransactionType.INCOME, category = CategoryType.SALARY)
        ), emptyList())
        val report = snapshot.calculateReport()
        assertEquals(200.0, report.summary.expense, 0.0)
        assertEquals(500.0, report.summary.income, 0.0)
        assertEquals(300.0, report.summary.remainingBalance, 0.0)
        assertEquals(listOf(CategoryType.FOOD, CategoryType.CLOTHING, CategoryType.TRANSPORT),
            report.categorySpending.map { it.category })
        assertEquals(listOf(50.0, 30.0, 20.0), report.categorySpending.map { it.percentage })
        assertEquals(CategoryType.FOOD, report.topSpendingCategories.single().category)
        assertTrue(report.budgetUsage.isEmpty())
    }

    @Test fun equallyHighestCategoriesAreAllPreserved() {
        val snapshot = FinancialSnapshot("USD", 150, 100, 200, listOf(
            expense(10.0, 110), expense(10.0, 120).copy(category = CategoryType.TRANSPORT),
            expense(5.0, 130).copy(category = CategoryType.CLOTHING)
        ), emptyList())
        val report = snapshot.calculateReport()
        assertEquals(setOf(CategoryType.FOOD, CategoryType.TRANSPORT),
            report.topSpendingCategories.map { it.category }.toSet())
    }

    @Test fun monthWithOnlyIncomeKeepsIncomeAndHasNoExpenseRanking() {
        val snapshot = FinancialSnapshot("USD", 150, 100, 200, listOf(
            expense(900.0, 99),
            expense(125.50, 120).copy(transaction = TransactionType.INCOME, category = CategoryType.SALARY)
        ), emptyList())
        val report = snapshot.calculateReport()
        assertEquals(125.50, report.summary.income, 0.0)
        assertEquals(0.0, report.summary.expense, 0.0)
        assertEquals(125.50, report.summary.remainingBalance, 0.0)
        assertEquals(1, report.transactions.size)
        assertTrue(report.categorySpending.isEmpty())
        assertTrue(report.topSpendingCategories.isEmpty())
    }

    @Test fun historyWithoutAnyCurrentMonthRecordProducesAnEmptyMonthlyReport() {
        val snapshot = FinancialSnapshot("USD", 150, 100, 200,
            listOf(expense(500.0, 99), expense(600.0, 200)), emptyList())
        val report = snapshot.calculateReport()
        assertEquals(0.0, report.summary.income, 0.0)
        assertEquals(0.0, report.summary.expense, 0.0)
        assertTrue(report.transactions.isEmpty())
        assertTrue(report.categorySpending.isEmpty())
    }
}

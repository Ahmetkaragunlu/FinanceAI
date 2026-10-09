package com.ahmetkaragunlu.financeai.feature.aichat.data.report

import android.content.Context
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.aichat.domain.model.FinancialSnapshot
import com.ahmetkaragunlu.financeai.feature.aichat.domain.report.CategorySpending
import com.ahmetkaragunlu.financeai.feature.aichat.domain.report.calculateReport
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toLabelResId
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class AiPromptFormatter @Inject constructor(@ApplicationContext private val context: Context) {
    fun format(question: String, snapshot: FinancialSnapshot): String {
        val report = snapshot.calculateReport()
        val zone = ZoneId.of(snapshot.calendarTimeZone)
        val startDate = Instant.ofEpochMilli(snapshot.monthStart).atZone(zone).toLocalDate()
        val endExclusiveDate = Instant.ofEpochMilli(snapshot.monthEndExclusive).atZone(zone).toLocalDate()
        fun categoryData(spending: CategorySpending) = mapOf(
            "category" to context.getString(spending.category.toLabelResId()),
            "amount" to spending.amount,
            "percentage" to spending.percentage
        )
        // JSON escapes notes/questions. They are data rather than part of the system instruction.
        return Gson().toJson(mapOf(
            "question" to question,
            "analysisScope" to "CURRENT_CALENDAR_MONTH",
            "periodYear" to startDate.year,
            "periodMonth" to startDate.monthValue,
            "periodStartDate" to startDate.toString(),
            "periodEndExclusiveDate" to endExclusiveDate.toString(),
            "currencyCode" to snapshot.currencyCode,
            "reportAt" to snapshot.at,
            "calendarTimeZone" to snapshot.calendarTimeZone,
            "monthStart" to snapshot.monthStart,
            "monthEndExclusive" to snapshot.monthEndExclusive,
            "income" to report.summary.income,
            "expense" to report.summary.expense,
            "balance" to report.summary.remainingBalance,
            "transactionCount" to report.transactions.size,
            "hasTransactions" to report.transactions.isNotEmpty(),
            "hasIncome" to report.transactions.any { it.transaction == TransactionType.INCOME },
            "hasExpenses" to report.transactions.any { it.transaction == TransactionType.EXPENSE },
            "hasBudgets" to report.budgetUsage.isNotEmpty(),
            "categorySpending" to report.categorySpending.map(::categoryData),
            "topSpendingCategories" to report.topSpendingCategories.map(::categoryData),
            "budgets" to report.budgetUsage.map { usage -> mapOf(
                "category" to usage.budget.category?.let { context.getString(it.toLabelResId()) },
                "limit" to usage.limit, "spent" to usage.spent, "usagePercentage" to usage.percentage,
                "canEvaluateLimit" to (usage.limit > 0)
            ) },
            "transactions" to report.transactions.map { row -> mapOf(
                "date" to row.date,
                "type" to context.getString(if (row.transaction == TransactionType.INCOME) R.string.income else R.string.expense),
                "category" to context.getString(row.category.toLabelResId()),
                "amount" to row.amount, "note" to row.note
            ) }
        ))
    }
}

package com.ahmetkaragunlu.financeai.feature.transaction.domain.model

import java.math.BigDecimal

/** Income and expense belong to one account/period snapshot. The ratio is remaining, not spent. */
data class FinancialSummary(val income: Double = 0.0, val expense: Double = 0.0) {
    val remainingBalance: Double
        get() = BigDecimal.valueOf(income).subtract(BigDecimal.valueOf(expense)).toDouble()
    val remainingIncomeRatio: Double
        get() = if (income > 0) remainingBalance / income else 0.0
}

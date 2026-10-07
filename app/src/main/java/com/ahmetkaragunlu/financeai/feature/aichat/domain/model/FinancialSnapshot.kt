package com.ahmetkaragunlu.financeai.feature.aichat.domain.model

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction

/** Account-scoped calendar-month records and configured budgets; formatting is outside the domain. */
data class FinancialSnapshot(
    val currencyCode: String,
    val at: Long,
    val monthStart: Long,
    val monthEndExclusive: Long,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val calendarTimeZone: String = "UTC"
)

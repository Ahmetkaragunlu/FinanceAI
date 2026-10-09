package com.ahmetkaragunlu.financeai.feature.budget.domain.calculation

/** Shared near-limit threshold; over-budget priority and financial calculations are separate. */
object BudgetWarningThreshold {
    const val PERCENTAGE = 80.0
    val progress: Float = (PERCENTAGE / 100.0).toFloat()
}

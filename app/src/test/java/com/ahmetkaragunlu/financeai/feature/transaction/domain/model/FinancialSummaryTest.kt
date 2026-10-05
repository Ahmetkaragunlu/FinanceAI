package com.ahmetkaragunlu.financeai.feature.transaction.domain.model

import org.junit.Assert.*
import org.junit.Test

class FinancialSummaryTest {
    @Test fun `fractional balance is retained without binary subtraction noise`() {
        assertEquals(100.2, FinancialSummary(300.3, 200.1).remainingBalance, 0.0)
    }
    @Test fun `ratio represents remaining income and does not hide overspending`() {
        assertEquals(0.75, FinancialSummary(100.0, 25.0).remainingIncomeRatio, 0.0)
        assertEquals(-0.5, FinancialSummary(100.0, 150.0).remainingIncomeRatio, 0.0)
    }
    @Test fun `zero income does not divide by zero or erase negative balance`() {
        assertEquals(0.0, FinancialSummary(0.0, 25.0).remainingIncomeRatio, 0.0)
        assertEquals(-25.0, FinancialSummary(0.0, 25.0).remainingBalance, 0.0)
    }
}

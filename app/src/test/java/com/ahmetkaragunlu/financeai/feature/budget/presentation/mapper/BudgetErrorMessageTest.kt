package com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetErrorMessageTest {
    @Test fun expectedFailuresUseSpecificLocalizedResources() {
        assertEquals(R.string.budget_rule_exists_error, budgetErrorMessageRes(BudgetException.DuplicateRule()))
        assertEquals(R.string.error_network_unavailable, budgetErrorMessageRes(DataAccessException.NetworkUnavailable()))
        assertEquals(R.string.error_stale_record, budgetErrorMessageRes(DataAccessException.StaleRecord()))
    }
    @Test fun diagnosticTextIsNotUsedAsTheUserMessage() {
        assertEquals(R.string.failure, budgetErrorMessageRes(IllegalStateException("private diagnostic")))
    }
    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToAFailure() { budgetErrorMessageRes(CancellationException()) }
}

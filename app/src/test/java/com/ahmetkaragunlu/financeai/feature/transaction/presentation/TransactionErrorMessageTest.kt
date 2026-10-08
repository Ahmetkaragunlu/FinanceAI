package com.ahmetkaragunlu.financeai.feature.transaction.presentation

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionErrorMessageTest {
    @Test
    fun failureRetainsOperationAndSafeDetailInsteadOfDiagnosticText() {
        val diagnostic = IllegalStateException("private financial diagnostic")
        assertEquals(
            TransactionActionResult.Failure(R.string.update_failed, R.string.error_access_denied),
            transactionFailure(DataAccessException.AccessDenied(diagnostic), R.string.update_failed),
        )
        assertEquals(
            TransactionActionResult.Failure(
                R.string.error_transaction_save_failed,
                R.string.error_operation_retry,
            ),
            transactionFailure(diagnostic, R.string.error_transaction_save_failed),
        )
    }

    @Test
    fun cancellationNeverBecomesAPendingUserError() {
        val cancelled = CancellationException("cancelled")
        assertSame(
            cancelled,
            assertThrows(CancellationException::class.java) {
                transactionFailure(cancelled, R.string.delete_failed)
            },
        )
    }
}

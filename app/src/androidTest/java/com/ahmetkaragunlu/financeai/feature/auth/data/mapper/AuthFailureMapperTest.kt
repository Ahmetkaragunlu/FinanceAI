package com.ahmetkaragunlu.financeai.feature.auth.data.mapper

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFailureMapperTest {
    @Test
    fun resetActionCodesAreTypedAndDoNotDependOnDiagnosticMessages() {
        val expired = FirebaseAuthException("ERROR_EXPIRED_ACTION_CODE", "same diagnostic")
        val invalid = FirebaseAuthException("ERROR_INVALID_ACTION_CODE", "same diagnostic")
        assertTrue(expired.toPasswordResetFailure() is AuthException.ExpiredResetCode)
        assertTrue(invalid.toPasswordResetFailure() is AuthException.InvalidResetCode)
        assertSame(expired, expired.toPasswordResetFailure().cause)
        val unknown = FirebaseAuthException("ERROR_OTHER", "ERROR_EXPIRED_ACTION_CODE")
        assertSame(unknown, unknown.toPasswordResetFailure())
        val network = FirebaseNetworkException("same diagnostic")
        assertTrue(network.toPasswordResetFailure() is DataAccessException.NetworkUnavailable)
        val cancelled = CancellationException()
        assertSame(cancelled, cancelled.toPasswordResetFailure())
        val typed = AuthException.InvalidResetCode()
        assertSame(typed, typed.toPasswordResetFailure())
    }
}

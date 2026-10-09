package com.ahmetkaragunlu.financeai.feature.auth.presentation.mapper

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthErrorMessageTest {
    @Test
    fun authSpecificFailuresUseSafeResourcesAndNeverDiagnosticText() {
        val cause = IllegalStateException("private diagnostic")
        assertEquals(R.string.this_email_is_already_exists, authErrorMessageRes(AuthException.EmailExists(cause)))
        assertEquals(R.string.invalid_email_or_password, authErrorMessageRes(AuthException.InvalidCredentials(cause)))
        assertEquals(R.string.user_not_found, authErrorMessageRes(AuthException.UidNotFound()))
        assertEquals(R.string.email_verification_could_not_be_sent, authErrorMessageRes(AuthException.VerificationEmailFailed(cause)))
        assertEquals(R.string.registration_incomplete_retry, authErrorMessageRes(AuthException.RegistrationIncomplete(cause)))
        assertEquals(R.string.something_went_wrong, authErrorMessageRes(AuthException.MissingGoogleEmail()))
        val cancelled = CancellationException()
        assertSame(cancelled, assertThrows(CancellationException::class.java) { authErrorMessageRes(cancelled) })
    }
    @Test
    fun expiredInvalidAndNetworkFailuresDoNotLoseTheirMeaning() {
        assertEquals(
            R.string.password_reset_link_expired,
            authErrorMessageRes(AuthException.ExpiredResetCode()),
        )
        assertEquals(
            R.string.password_reset_link_invalid,
            authErrorMessageRes(AuthException.InvalidResetCode()),
        )
        assertEquals(
            R.string.error_network_unavailable,
            authErrorMessageRes(DataAccessException.NetworkUnavailable()),
        )
        assertEquals(
            R.string.something_went_wrong,
            authErrorMessageRes(IllegalStateException("private diagnostic")),
        )
    }
}

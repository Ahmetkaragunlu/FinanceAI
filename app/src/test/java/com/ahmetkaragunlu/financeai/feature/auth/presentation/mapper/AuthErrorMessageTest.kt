package com.ahmetkaragunlu.financeai.feature.auth.presentation.mapper

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMessageTest {
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

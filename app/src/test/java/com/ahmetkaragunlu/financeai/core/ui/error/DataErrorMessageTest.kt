package com.ahmetkaragunlu.financeai.core.ui.error

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class DataErrorMessageTest {
    @Test
    fun expectedFailuresKeepDistinctSafeUserMessages() {
        val diagnostic = IllegalStateException("private diagnostic")
        assertEquals(
            R.string.error_request_timed_out,
            dataErrorMessageRes(DataAccessException.TimedOut(diagnostic)),
        )
        assertEquals(
            R.string.error_service_unavailable,
            dataErrorMessageRes(DataAccessException.ServiceUnavailable(diagnostic)),
        )
        assertEquals(
            R.string.error_network_unavailable,
            dataErrorMessageRes(DataAccessException.NetworkUnavailable(diagnostic)),
        )
        assertEquals(
            R.string.error_access_denied,
            dataErrorMessageRes(DataAccessException.AccessDenied(diagnostic)),
        )
        assertNull(dataErrorMessageRes(diagnostic))
        assertEquals(R.string.error_rate_limited, dataErrorMessageRes(DataAccessException.RateLimited(diagnostic)))
        assertEquals(R.string.error_invalid_remote_data, dataErrorMessageRes(DataAccessException.InvalidRemoteData(diagnostic)))
        assertEquals(R.string.error_stale_record, dataErrorMessageRes(DataAccessException.StaleRecord()))
    }

    @Test
    fun cancellationIsPropagatedInsteadOfDisplayedAsFailure() {
        val cancelled = CancellationException("operation cancelled")
        assertSame(
            cancelled,
            assertThrows(CancellationException::class.java) { dataErrorMessageRes(cancelled) },
        )
    }
}

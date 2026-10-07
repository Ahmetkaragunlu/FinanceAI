package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class AiFailureMapperTest {
    @Test fun networkFailureKeepsItsCauseWithoutDiagnosticMessageMatching() {
        val network = IOException("diagnostic")
        val failure = network.toAiFailure()
        assertTrue(failure is AiException.NetworkUnavailable)
        assertSame(network, failure.cause)
        val typed = AiException.RateLimited()
        assertSame(typed, typed.toAiFailure())
    }
    @Test fun cancellationAndProgrammingInvariantKeepTheirIdentity() {
        val cancellation = CancellationException()
        val invariant = IllegalStateException("invariant")
        assertSame(cancellation, cancellation.toAiFailure())
        assertSame(invariant, invariant.toAiFailure())
    }

    @Test fun wrappedCancellationIsNotReportedAsAServiceFailure() {
        val cancellation = CancellationException("cancel")
        val wrapper = RuntimeException("SDK wrapper", cancellation)
        assertSame(cancellation, wrapper.toAiFailure())
    }
}

package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.InvalidLocationException
import com.google.firebase.ai.type.InvalidStateException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.ServiceConnectionHandshakeFailedException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class AiFailureMapperTest {
    @Test fun sdkTypesDetermineExpectedFailuresWithoutInstantiatingInternalConstructors() {
        val rateLimited = mock(QuotaExceededException::class.java)
        val timeout = mock(RequestTimeoutException::class.java)
        val unavailable = mock(ServerException::class.java)
        assertTrue(rateLimited.toAiFailure() is AiException.RateLimited)
        assertTrue(timeout.toAiFailure() is AiException.TimedOut)
        assertTrue(unavailable.toAiFailure() is AiException.ServiceUnavailable)
        assertSame(rateLimited, rateLimited.toAiFailure().cause)
        listOf(PromptBlockedException::class.java, ContentBlockedException::class.java,
            ResponseStoppedException::class.java).forEach {
            val original = mock(it)
            val mapped = original.toAiFailure()
            assertTrue(mapped is AiException.ResponseRejected)
            assertSame(original, mapped.cause)
        }
        listOf(InvalidAPIKeyException::class.java, APINotConfiguredException::class.java,
            ServiceDisabledException::class.java, InvalidLocationException::class.java,
            UnsupportedUserLocationException::class.java).forEach {
            val original = mock(it)
            val mapped = original.toAiFailure()
            assertTrue(mapped is AiException.Configuration)
            assertSame(original, mapped.cause)
        }
        val invariant = mock(InvalidStateException::class.java)
        assertSame(invariant, invariant.toAiFailure())
    }

    @Test fun sdkFallbackKeepsNetworkAndCancellationCausesDistinct() {
        val unknown = ServiceConnectionHandshakeFailedException("same diagnostic")
        assertTrue(unknown.toAiFailure() is AiException.Unavailable)
        assertSame(unknown, unknown.toAiFailure().cause)
        val network = IOException("diagnostic")
        val wrapped = ServiceConnectionHandshakeFailedException("same diagnostic", network)
        assertTrue(wrapped.toAiFailure() is AiException.NetworkUnavailable)
        val cancellation = CancellationException()
        assertSame(cancellation, ServiceConnectionHandshakeFailedException("same diagnostic", cancellation).toAiFailure())
    }
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

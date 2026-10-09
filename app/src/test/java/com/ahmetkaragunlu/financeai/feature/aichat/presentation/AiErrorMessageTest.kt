package com.ahmetkaragunlu.financeai.feature.aichat.presentation

import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AiErrorMessageTest {
    @Test fun expectedFailuresKeepExistingSafeMessagesWithoutUsingDiagnostics() {
        listOf(AiException.RateLimited() to R.string.error_rate_limited,
            AiException.ResponseRejected() to R.string.ai_request_rejected,
            AiException.Configuration() to R.string.ai_configuration_error,
            AiException.EmptyResponse() to R.string.ai_response_error_empty,
            AiException.AccessVerification() to R.string.ai_access_verification_failed,
            AiException.TimedOut() to R.string.ai_request_timed_out,
            AiException.NetworkUnavailable() to R.string.ai_network_unavailable,
            AiException.ServiceUnavailable() to R.string.ai_service_unavailable,
            AiException.Unavailable() to R.string.ai_request_failed,
            IllegalStateException("private") to R.string.ai_request_failed).forEach { (error, resource) ->
            assertEquals(resource, aiErrorMessageRes(error))
        }
    }
    @Test fun cancellationIsNeverRenderedAsAnAiFailure() {
        val cancelled = CancellationException()
        assertSame(cancelled, assertThrows(CancellationException::class.java) { aiErrorMessageRes(cancelled) })
    }
}

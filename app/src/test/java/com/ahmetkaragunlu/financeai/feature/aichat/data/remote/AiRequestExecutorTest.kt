package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiRequestExecutorTest {
    private val executor = AiRequestExecutor()

    @Test fun temporaryFailureRetriesOnceAndReturnsOnlyTheSuccessfulResult() = runTest {
        var calls = 0
        val answer = executor.execute {
            calls++
            if (calls == 1) throw AiException.ServiceUnavailable()
            "answer"
        }
        assertEquals("answer", answer)
        assertEquals(2, calls)
        assertTrue(testScheduler.currentTime in 1_000L..1_500L)
    }

    @Test fun persistentNetworkFailureStopsAfterTwoAttempts() = runTest {
        var calls = 0
        val failure = AiException.NetworkUnavailable()
        try {
            executor.execute { calls++; throw failure }
            fail("Expected failure")
        } catch (actual: AiException.NetworkUnavailable) { assertEquals(failure.javaClass, actual.javaClass) }
        assertEquals(2, calls)
    }

    @Test fun accessQuotaBlockedAndEmptyResponsesAreNeverAutomaticallyRetried() = runTest {
        for (failure in listOf(AiException.AccessVerification(), AiException.Configuration(),
            AiException.RateLimited(), AiException.ResponseRejected(), AiException.EmptyResponse())) {
            var calls = 0
            try {
                executor.execute { calls++; throw failure }
                fail("Expected failure")
            } catch (actual: AiException) { assertEquals(failure.javaClass, actual.javaClass) }
            assertEquals(1, calls)
        }
    }

    @Test fun requestThatNeverRespondsStopsWithATypedTimeout() = runTest {
        var calls = 0
        try {
            executor.execute { calls++; awaitCancellation() }
            fail("Expected timeout")
        } catch (_: AiException.TimedOut) { }
        assertEquals(2, calls)
        assertTrue(testScheduler.currentTime <= AiRequestExecutor.TOTAL_TIMEOUT_MILLIS)
    }

    @Test fun cancellationAndProgrammingErrorsAreNotRetriedOrConvertedToTimeouts() = runTest {
        for (failure in listOf(CancellationException("cancel"), IllegalStateException("invariant"))) {
            var calls = 0
            try {
                executor.execute { calls++; throw failure }
                fail("Expected exception")
            } catch (actual: Exception) { assertEquals(failure.javaClass, actual.javaClass) }
            assertEquals(1, calls)
        }
    }

    @Test fun cancellationDuringBackoffStopsTheNextModelCall() = runTest {
        var calls = 0
        val job = async { executor.execute { calls++; throw AiException.ServiceUnavailable() } }
        runCurrent()
        assertEquals(1, calls)
        job.cancel()
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(1, calls)
        assertTrue(job.isCancelled)
    }
}

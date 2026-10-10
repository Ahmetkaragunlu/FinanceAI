package com.ahmetkaragunlu.financeai.feature.aichat.data.remote.generation

import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Bounded model attempts; the caller owns the single persisted request/reply. */
class AiRequestExecutor @Inject constructor() {
    suspend fun execute(request: suspend () -> String): String =
        withTimeoutOrNull(TOTAL_TIMEOUT_MILLIS) {
            for (attempt in 0 until MAX_ATTEMPTS) {
                try {
                    return@withTimeoutOrNull withTimeoutOrNull(ATTEMPT_TIMEOUT_MILLIS) { request() }
                        ?: throw AiException.TimedOut()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: AiException) {
                    val canRetry = e is AiException.ServiceUnavailable ||
                        e is AiException.NetworkUnavailable || e is AiException.TimedOut
                    if (!canRetry || attempt == MAX_ATTEMPTS - 1) throw e
                }
                delay(Random.nextLong(1_000L, 1_501L))
            }
            error("Model attempt loop did not return a result")
        } ?: throw AiException.TimedOut()

    companion object {
        internal const val ATTEMPT_TIMEOUT_MILLIS = 25_000L
        internal const val TOTAL_TIMEOUT_MILLIS = 55_000L
        private const val MAX_ATTEMPTS = 2
    }
}

package com.ahmetkaragunlu.financeai.feature.aichat.presentation

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import kotlinx.coroutines.CancellationException

@StringRes
fun aiErrorMessageRes(error: Throwable): Int = when (error) {
    is CancellationException -> throw error
    is AiException.RateLimited -> R.string.error_rate_limited
    is AiException.ResponseRejected -> R.string.ai_request_rejected
    is AiException.Configuration -> R.string.ai_configuration_error
    is AiException.EmptyResponse -> R.string.ai_response_error_empty
    is AiException.AccessVerification -> R.string.ai_access_verification_failed
    is AiException.TimedOut -> R.string.ai_request_timed_out
    is AiException.NetworkUnavailable -> R.string.ai_network_unavailable
    is AiException.ServiceUnavailable -> R.string.ai_service_unavailable
    else -> R.string.ai_request_failed
}

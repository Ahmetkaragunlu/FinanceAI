package com.ahmetkaragunlu.financeai.feature.aichat.data.remote

import com.ahmetkaragunlu.financeai.feature.aichat.domain.error.AiException
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.InvalidLocationException
import com.google.firebase.ai.type.InvalidStateException
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Use SDK types, never provider diagnostic text, to decide expected failure behaviour. */
fun Exception.toAiFailure(): Exception {
    generateSequence<Throwable>(this) { it.cause }
        .filterIsInstance<CancellationException>().firstOrNull()?.let { return it }
    return when (this) {
        is CancellationException, is AiException -> this
        is QuotaExceededException -> AiException.RateLimited(this)
        is PromptBlockedException, is ContentBlockedException, is ResponseStoppedException -> AiException.ResponseRejected(this)
        is InvalidAPIKeyException, is APINotConfiguredException, is ServiceDisabledException,
        is InvalidLocationException, is UnsupportedUserLocationException -> AiException.Configuration(this)
        is InvalidStateException -> this
        is RequestTimeoutException -> AiException.TimedOut(this)
        is ServerException -> AiException.ServiceUnavailable(this)
        is IOException -> AiException.NetworkUnavailable(this)
        is FirebaseAIException -> {
            if (generateSequence<Throwable>(this) { it.cause }.any { it is IOException })
                AiException.NetworkUnavailable(this)
            else AiException.Unavailable(this)
        }
        else -> this
    }
}

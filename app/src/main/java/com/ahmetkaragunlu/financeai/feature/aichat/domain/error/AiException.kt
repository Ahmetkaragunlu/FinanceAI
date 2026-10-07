package com.ahmetkaragunlu.financeai.feature.aichat.domain.error

sealed class AiException(cause: Throwable? = null) : Exception(cause) {
    class Unavailable(cause: Throwable? = null) : AiException(cause)
    class NetworkUnavailable(cause: Throwable? = null) : AiException(cause)
    class ServiceUnavailable(cause: Throwable? = null) : AiException(cause)
    class TimedOut(cause: Throwable? = null) : AiException(cause)
    class AccessVerification(cause: Throwable? = null) : AiException(cause)
    class EmptyResponse : AiException()
    class ResponseRejected(cause: Throwable? = null) : AiException(cause)
    class Configuration(cause: Throwable? = null) : AiException(cause)
    class RateLimited(cause: Throwable? = null) : AiException(cause)
}

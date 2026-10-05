package com.ahmetkaragunlu.financeai.core.error

/** Expected cross-feature data failures. Diagnostic causes are never user-facing messages. */
sealed class DataAccessException(cause: Throwable? = null) : Exception(cause) {
    class NetworkUnavailable(cause: Throwable? = null) : DataAccessException(cause)
    class AccessDenied(cause: Throwable? = null) : DataAccessException(cause)
    class RateLimited(cause: Throwable? = null) : DataAccessException(cause)
    class InvalidRemoteData(cause: Throwable? = null) : DataAccessException(cause)
    class StaleRecord : DataAccessException()
}

package com.ahmetkaragunlu.financeai.feature.auth.domain.error

sealed class AuthException(cause: Throwable? = null) : Exception(cause) {
    class EmailExists(cause: Throwable? = null) : AuthException(cause)

    class UidNotFound : AuthException()

    class VerificationEmailFailed(cause: Throwable? = null) : AuthException(cause)

    class InvalidCredentials(cause: Throwable? = null) : AuthException(cause)

    class MissingGoogleEmail : AuthException()

    class ExpiredResetCode(cause: Throwable? = null) : AuthException(cause)

    class InvalidResetCode(cause: Throwable? = null) : AuthException(cause)
}

package com.ahmetkaragunlu.financeai.feature.auth.data.mapper

import com.ahmetkaragunlu.financeai.core.firebase.error.toDataAccessFailure
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.google.firebase.auth.FirebaseAuthException

/** Firebase's action-code strings are translated once at the password-reset SDK boundary. */
fun Exception.toPasswordResetFailure(): Exception =
    when {
        this is FirebaseAuthException && errorCode == "ERROR_EXPIRED_ACTION_CODE" ->
            AuthException.ExpiredResetCode(this)

        this is FirebaseAuthException && errorCode == "ERROR_INVALID_ACTION_CODE" ->
            AuthException.InvalidResetCode(this)

        else -> toDataAccessFailure()
    }

package com.ahmetkaragunlu.financeai.core.firebase

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException

/** Map only recognised SDK failures at a data boundary; preserve cancellation and unknown causes. */
fun Exception.toDataAccessFailure(): Exception = when (this) {
    is CancellationException -> this
    is DataAccessException -> this
    is FirebaseNetworkException -> DataAccessException.NetworkUnavailable(this)
    is FirebaseTooManyRequestsException -> DataAccessException.RateLimited(this)
    is FirebaseFunctionsException -> when (code) {
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> DataAccessException.NetworkUnavailable(this)
        FirebaseFunctionsException.Code.PERMISSION_DENIED,
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> DataAccessException.AccessDenied(this)
        FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> DataAccessException.RateLimited(this)
        else -> this
    }
    else -> this
}

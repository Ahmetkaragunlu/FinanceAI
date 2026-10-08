package com.ahmetkaragunlu.financeai.core.firebase.error

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.StorageException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

/**
 * Map only recognised SDK failures at a data boundary; preserve cancellation and unknown causes.
 */
fun Exception.toDataAccessFailure(): Exception =
    when (this) {
        is CancellationException -> this
        is DataAccessException -> this
        is FirebaseNetworkException -> DataAccessException.NetworkUnavailable(this)
        is FirebaseTooManyRequestsException -> DataAccessException.RateLimited(this)
        is SocketTimeoutException -> DataAccessException.TimedOut(this)
        is FirebaseFunctionsException ->
            when (code) {
                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    DataAccessException.ServiceUnavailable(this)
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    DataAccessException.TimedOut(this)
                FirebaseFunctionsException.Code.PERMISSION_DENIED,
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    DataAccessException.AccessDenied(this)
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    DataAccessException.RateLimited(this)
                else -> this
            }
        is FirebaseFirestoreException ->
            when (code) {
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    DataAccessException.TimedOut(this)
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    DataAccessException.ServiceUnavailable(this)
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    DataAccessException.AccessDenied(this)
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    DataAccessException.RateLimited(this)
                else -> this
            }
        is StorageException ->
            when (errorCode) {
                StorageException.ERROR_NOT_AUTHENTICATED,
                StorageException.ERROR_NOT_AUTHORIZED -> DataAccessException.AccessDenied(this)
                StorageException.ERROR_QUOTA_EXCEEDED -> DataAccessException.RateLimited(this)
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> DataAccessException.TimedOut(this)
                else -> this
            }
        else -> this
    }

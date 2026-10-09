package com.ahmetkaragunlu.financeai.feature.auth.data.remote

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.UserFields
import com.ahmetkaragunlu.financeai.core.firebase.error.toDataAccessFailure
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** Pre-login checks return only the existing workflow's boolean, never public profile documents. */
class AuthLookupRemote @Inject constructor(private val functions: FirebaseFunctions) {
    suspend fun registered(email: String): Boolean =
        call("checkRegisteredAccount", mapOf(UserFields.EMAIL to email))

    suspend fun resetIdentity(email: String, firstName: String, lastName: String): Boolean =
        call(
            "checkPasswordResetIdentity",
            mapOf(UserFields.EMAIL to email, UserFields.FIRST_NAME to firstName, UserFields.LAST_NAME to lastName),
        )

    private suspend fun call(name: String, input: Map<String, String>): Boolean =
        try {
            val response =
                functions.getHttpsCallable(name).call(input).await().data as? Map<*, *>
                    ?: throw DataAccessException.InvalidRemoteData()
            response["found"] as? Boolean ?: throw DataAccessException.InvalidRemoteData()
        } catch (e: Exception) {
            throw e.toDataAccessFailure()
        }
}

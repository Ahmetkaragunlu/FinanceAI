package com.ahmetkaragunlu.financeai.feature.auth.data.remote

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.error.toDataAccessFailure
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** Pre-login checks return only the existing workflow's boolean, never public profile documents. */
class AuthLookupRemote @Inject constructor(private val functions: FirebaseFunctions) {
    suspend fun registered(email: String): Boolean =
        call("checkRegisteredAccount", mapOf("email" to email))

    suspend fun resetIdentity(email: String, firstName: String, lastName: String): Boolean =
        call(
            "checkPasswordResetIdentity",
            mapOf("email" to email, "firstName" to firstName, "lastName" to lastName),
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

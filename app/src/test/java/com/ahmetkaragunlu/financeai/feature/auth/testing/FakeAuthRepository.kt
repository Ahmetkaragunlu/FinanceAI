package com.ahmetkaragunlu.financeai.feature.auth.testing

import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository

/** Shared by the registration/reset/session tests; unexpected calls fail instead of pretending success. */
class FakeAuthRepository : AuthRepository {
    data class Registration(val email: String, val password: String, val firstName: String, val lastName: String)
    data class ResetRequest(val email: String, val firstName: String, val lastName: String)

    var registration: Registration? = null
    var registrationFailure: Exception? = null
    var registrationCalls = 0
    var onRegistration: suspend () -> Unit = {}
    var resetRequest: ResetRequest? = null
    var resetRequestMatches = true
    var resetRequestFailure: Exception? = null
    var resetRequestCalls = 0
    var onResetRequest: suspend () -> Unit = {}
    var resetConfirmation: Pair<String, String>? = null
    var resetConfirmationFailure: Exception? = null
    var resetConfirmationCalls = 0
    var onResetConfirmation: suspend () -> Unit = {}
    var onSignOut: suspend () -> Unit = {}

    override suspend fun saveUser(email: String, password: String, firstName: String, lastName: String) {
        registrationCalls++
        registration = Registration(email, password, firstName, lastName)
        onRegistration()
        registrationFailure?.let { throw it }
    }

    override suspend fun verifyUserAndSendResetEmail(email: String, firstName: String, lastName: String): Boolean {
        resetRequestCalls++
        resetRequest = ResetRequest(email, firstName, lastName)
        onResetRequest()
        resetRequestFailure?.let { throw it }
        return resetRequestMatches
    }

    override suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        resetConfirmationCalls++
        resetConfirmation = oobCode to newPassword
        onResetConfirmation()
        resetConfirmationFailure?.let { throw it }
    }

    override suspend fun signOut() = onSignOut()
    override suspend fun signIn(email: String, password: String): Unit = unexpected()
    override suspend fun refreshEmailVerification(): Boolean = unexpected()
    override suspend fun signInWithGoogle(idToken: String?): Unit = unexpected()
    override suspend fun isUserRegistered(email: String): Boolean = unexpected()
    override suspend fun getUserName(): String? = unexpected()

    private fun unexpected(): Nothing = error("Unexpected auth repository call")
}

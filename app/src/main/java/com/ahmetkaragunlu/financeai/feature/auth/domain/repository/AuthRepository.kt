package com.ahmetkaragunlu.financeai.feature.auth.domain.repository

interface AuthRepository {
    suspend fun signIn(email: String, password: String)
    suspend fun refreshEmailVerification(): Boolean
    suspend fun saveUser(email: String, password: String, firstName: String, lastName: String)
    suspend fun verifyUserAndSendResetEmail(email: String, firstName: String, lastName: String): Boolean
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String)
    suspend fun signInWithGoogle(idToken: String?)
    suspend fun isUserRegistered(email: String): Boolean
    suspend fun signOut()
    suspend fun getUserName(): String?

}

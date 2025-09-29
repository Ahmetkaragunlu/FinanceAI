package com.ahmetkaragunlu.financeai.firebaseRepo

import com.ahmetkaragunlu.financeai.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult

interface AuthRepository {
    val currentUser: Any?
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun saveUser(email: String, firstName: String, lastName: String, password: String)
    suspend fun signInWithGoogle(account: GoogleSignInAccount)
    suspend fun saveUserFirestore(user: User)
    suspend fun logOut()
    suspend fun sendPasswordResetEmail(firstName: String, lastName: String, email: String)
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String)
    suspend fun sendEmailVerification()
    suspend fun checkEmailVerified(): Boolean
    suspend fun deleteUnverifiedUser()


}
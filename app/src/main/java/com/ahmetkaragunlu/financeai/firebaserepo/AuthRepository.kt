

package com.ahmetkaragunlu.financeai.firebaserepo

import com.ahmetkaragunlu.financeai.firebasemodel.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult

interface AuthRepository {
    val currentUser: Any?
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun saveUser(email: String, password: String, firstName: String, lastName: String)
    suspend fun saveUserFirestore(user: User)
    suspend fun sendEmailVerification()
    suspend fun verifyUserAndSendResetEmail(email: String, firstName: String, lastName: String): Boolean
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String)
    suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult
    suspend fun isUserRegistered(email: String): Boolean
    suspend fun signOut()

}

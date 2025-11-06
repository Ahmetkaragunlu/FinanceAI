package com.ahmetkaragunlu.financeai.firebaserepo

import com.ahmetkaragunlu.financeai.firebasemodel.User
import com.ahmetkaragunlu.financeai.screens.auth.AuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseSyncService: FirebaseSyncService
) : AuthRepository {

    override val currentUser get() = auth.currentUser

    override suspend fun signUp(email: String, password: String): AuthResult =
        auth.createUserWithEmailAndPassword(email, password).await()

    override suspend fun signIn(email: String, password: String): AuthResult =
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            firebaseSyncService.initializeSyncAfterLogin()

            result
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> {
                    throw AuthException.InvalidCredentials
                }
                else -> throw e
            }
        }

    override suspend fun saveUserFirestore(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }

    override suspend fun saveUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) {
        try {
            val authResult = signUp(email = email, password = password)
            sendEmailVerification()
            val uid = authResult.user?.uid ?: throw AuthException.UidNotFound
            val user = User(email = email, firstName = firstName, lastName = lastName, uid = uid)
            saveUserFirestore(user)
            firebaseSyncService.initializeSyncAfterLogin()
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthUserCollisionException -> {
                    throw AuthException.EmailExists
                }
                else -> throw e
            }
        }
    }

    override suspend fun sendEmailVerification() {
        try {
            auth.currentUser?.sendEmailVerification()?.await()
        } catch (e: Exception) {
            throw AuthException.VerificationEmailFailed
        }
    }

    override suspend fun verifyUserAndSendResetEmail(
        email: String,
        firstName: String,
        lastName: String
    ): Boolean {
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("firstName", firstName)
            .whereEqualTo("lastName", lastName)
            .get()
            .await()
        return if (!snapshot.isEmpty) {
            auth.sendPasswordResetEmail(email).await()
            true
        } else false
    }

    override suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        auth.confirmPasswordReset(oobCode, newPassword).await()
    }

    override suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()
        firebaseSyncService.initializeSyncAfterLogin()
        return result
    }

    override suspend fun isUserRegistered(email: String): Boolean {
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    override suspend fun signOut() {
        firebaseSyncService.resetSync()
        auth.signOut()
    }
}
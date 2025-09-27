package com.ahmetkaragunlu.financeai.firebaseRepo

import android.util.Log
import com.ahmetkaragunlu.financeai.model.User
import com.ahmetkaragunlu.financeai.screens.auth.AuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override val currentUser get() = auth.currentUser

    override suspend fun signUp(email: String, password: String): AuthResult =
        auth.createUserWithEmailAndPassword(email, password).await()

    override suspend fun signIn(email: String, password: String): AuthResult =
        auth.signInWithEmailAndPassword(email, password).await()

    override suspend fun logOut() {
        auth.signOut()
    }

    override suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        val googleIdToken = account.idToken ?: throw AuthException.IdTokenIsNull
        try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(credential).await()

            val email = authResult.user?.email ?: throw AuthException.IdTokenIsNull
            val userQuery = firestore
                .collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (userQuery.isEmpty) {
                authResult.user?.delete()?.await()
                throw AuthException.UserNotRegistered
            }
            userQuery.documents.firstOrNull()?.reference?.update(
                "lastSignIn", com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

        } catch (e: Exception) {
            try {
                auth.currentUser?.delete()?.await()
            } catch (deleteException: Exception) {
                auth.signOut()
            }
            when (e) {
                is AuthException.UserNotRegistered -> {
                    throw e
                }

                is AuthException.IdTokenIsNull -> {
                    throw e
                }

                else -> {
                    throw AuthException.Unknown(e.message)
                }
            }
        }
    }

    override suspend fun sendPasswordResetEmail(
        firstName: String,
        lastName: String,
        email: String
    ) {
        try {
            val userQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("firstName", firstName)
                .whereEqualTo("lastName", lastName)
                .limit(1)
                .get()
                .await()

            if (userQuery.isEmpty) {
                throw AuthException.UserNotRegistered
            }

            auth.sendPasswordResetEmail(email).await()

        } catch (e: AuthException.UserNotRegistered) {
            throw e
        } catch (e: Exception) {
            throw AuthException.Unknown(e.message)
        }
    }

    override suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        try {
            auth.confirmPasswordReset(oobCode, newPassword).await()
        } catch (e: Exception) {
            when (e) {
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                    throw AuthException.InvalidOobCode
                }

                else -> {
                    throw AuthException.Unknown(e.message)
                }
            }
        }
    }


    override suspend fun saveUserFirestore(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }

    override suspend fun saveUser(
        email: String,
        firstName: String,
        lastName: String,
        password: String
    ) {

        val authResult = try {
            signUp(email = email, password = password)
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AuthException.EmailExists
        }

        val uid = authResult.user?.uid ?: throw AuthException.UidNotFound
        val user = User(firstName = firstName, lastName = lastName, email = email, uid = uid)
        saveUserFirestore(user)
    }
}
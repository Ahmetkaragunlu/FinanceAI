package com.ahmetkaragunlu.financeai.feature.auth.data.repository

import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.User
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val coordinator: SessionCoordinator,
    private val fcmTokenManager: FCMTokenManager,
) : AuthRepository {
    private val transitions = Mutex()

    // Firebase Tasks cannot be undone by cancelling the caller. Keep transitions serialized until SDK completion.
    private suspend fun <T> completeAuth(task: Task<T>): T =
        withContext(NonCancellable) { task.await() }

    private suspend fun signUp(email: String, password: String): AuthResult =
        completeAuth(auth.createUserWithEmailAndPassword(email, password))

    override suspend fun signIn(email: String, password: String): Unit = transitions.withLock {
        try {
            completeAuth(auth.signInWithEmailAndPassword(email, password))
            coordinator.prepare()
            withTimeoutOrNull(2_000) { fcmTokenManager.updateFCMToken() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> {
                    throw AuthException.InvalidCredentials
                }

                else -> throw e
            }
        }
    }

    override suspend fun refreshEmailVerification(): Boolean = transitions.withLock {
        auth.currentUser?.reload()?.await()
        val verified = auth.currentUser?.isEmailVerified == true
        if (verified) coordinator.prepare()
        verified
    }

    private suspend fun saveUserFirestore(user: User) {
        firestore.collection("users").document(user.uid).set(user, SetOptions.merge()).await()
    }

    override suspend fun saveUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Unit = transitions.withLock {
        try {
            val authResult = signUp(email = email, password = password)
            sendEmailVerification()
            val uid = authResult.user?.uid ?: throw AuthException.UidNotFound
            val user = User(
                email = email,
                firstName = firstName,
                lastName = lastName,
                uid = uid,
                fcmTokens = emptyList()
            )
            saveUserFirestore(user)
            coordinator.prepare()
            withTimeoutOrNull(2_000) { fcmTokenManager.updateFCMToken() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            when (e) {
                is FirebaseAuthUserCollisionException -> {
                    throw AuthException.EmailExists
                }

                else -> throw e
            }
        }
    }

    private suspend fun sendEmailVerification() {
        try {
            auth.currentUser?.sendEmailVerification()?.await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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

    override suspend fun signInWithGoogle(idToken: String?): Unit = transitions.withLock {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        completeAuth(auth.signInWithCredential(credential))
        coordinator.prepare()
        withTimeoutOrNull(2_000) { fcmTokenManager.updateFCMToken() }
    }

    override suspend fun isUserRegistered(email: String): Boolean {
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .get(Source.SERVER)
            .await()
        return !snapshot.isEmpty
    }

    override suspend fun getUserName(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (auth.currentUser?.uid == uid) document.getString("firstName") else null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun signOut() = transitions.withLock {
        // Bound remote token cleanup; a network outage must not prevent local sign-out.
        withTimeoutOrNull(2_000) { fcmTokenManager.removeFCMToken() }
        coordinator.signOut()
    }
}

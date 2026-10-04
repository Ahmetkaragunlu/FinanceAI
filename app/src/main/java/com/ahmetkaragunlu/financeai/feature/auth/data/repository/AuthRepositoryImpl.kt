package com.ahmetkaragunlu.financeai.feature.auth.data.repository

import androidx.work.WorkManager
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.User
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseSyncService: FirebaseSyncService,
    private val fcmTokenManager: FCMTokenManager,
    private val database: FinanceDatabase,
    private val workManager: WorkManager,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private suspend fun signUp(email: String, password: String): AuthResult =
        auth.createUserWithEmailAndPassword(email, password).await()

    override suspend fun signIn(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            firebaseSyncService.initializeSyncAfterLogin()
            fcmTokenManager.updateFCMToken()
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> {
                    throw AuthException.InvalidCredentials
                }

                else -> throw e
            }
        }
    }

    override suspend fun refreshEmailVerification(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified == true
    }

    private suspend fun saveUserFirestore(user: User) {
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
            val user = User(
                email = email,
                firstName = firstName,
                lastName = lastName,
                uid = uid,
                fcmTokens = emptyList()
            )
            saveUserFirestore(user)
            firebaseSyncService.initializeSyncAfterLogin()
            fcmTokenManager.updateFCMToken()
        } catch (e: Exception) {
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

    override suspend fun signInWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        firebaseSyncService.initializeSyncAfterLogin()
        fcmTokenManager.updateFCMToken()
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
            document.getString("firstName")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signOut() {
        fcmTokenManager.removeFCMToken()
        firebaseSyncService.resetSync()
        workManager.cancelAllWork()
        auth.signOut()
        scope.launch {
            database.clearAllTables()
        }
    }
}

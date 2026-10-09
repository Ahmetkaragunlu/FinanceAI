package com.ahmetkaragunlu.financeai.feature.auth.data.repository

import android.util.Log
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.firebase.FirestoreCollections
import com.ahmetkaragunlu.financeai.core.firebase.UserFields
import com.ahmetkaragunlu.financeai.core.firebase.error.toDataAccessFailure
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.auth.data.local.session.CredentialSessionCleaner
import com.ahmetkaragunlu.financeai.feature.auth.data.mapper.toPasswordResetFailure
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.AuthLookupRemote
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.User
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepositoryImpl
@Inject
constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val coordinator: SessionCoordinator,
    private val fcmTokenManager: FCMTokenManager,
    private val lookup: AuthLookupRemote,
    private val credentialSessionCleaner: CredentialSessionCleaner,
) : AuthRepository {
    private val transitions = Mutex()
    private var pendingRegistration: RegistrationAttempt? = null

    // Retry authority comes from this successful SDK creation, never from an email collision.
    // Kept only for this repository lifetime; no password or SDK token is retained.
    private class RegistrationAttempt(val submittedEmail: String, val profile: User) {
        var verificationSent = false
        var profileSaved = false
    }

    // Firebase Tasks cannot be undone by cancelling the caller. Keep transitions serialized until
    // SDK completion.
    private suspend fun <T> completeAuth(task: Task<T>): T =
        withContext(NonCancellable) { task.await() }

    override suspend fun signIn(email: String, password: String): Unit =
        transitions.withLock {
            pendingRegistration = null
            try {
                completeAuth(auth.signInWithEmailAndPassword(email, password))
                coordinator.prepare()
                queueToken()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is FirebaseAuthInvalidUserException,
                    is FirebaseAuthInvalidCredentialsException -> {
                        throw AuthException.InvalidCredentials(e)
                    }

                    else -> throw e.toDataAccessFailure()
                }
            }
        }

    override suspend fun refreshEmailVerification(): Boolean =
        transitions.withLock {
            try {
                auth.currentUser?.reload()?.await()
                val verified = auth.currentUser?.isEmailVerified == true
                if (verified) coordinator.prepare()
                verified
            } catch (e: Exception) {
                throw e.toDataAccessFailure()
            }
        }

    private suspend fun saveUserFirestore(user: User) {
        val ref = firestore.collection(FirestoreCollections.USERS).document(user.uid)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(ref)
            if (existing.contains(UserFields.UID) && existing.getString(UserFields.UID) != user.uid) {
                throw DataAccessException.InvalidRemoteData()
            }
            val fields = mapOf(
                UserFields.UID to user.uid,
                UserFields.EMAIL to user.email,
                UserFields.FIRST_NAME to user.firstName,
                UserFields.LAST_NAME to user.lastName,
                UserFields.FCM_TOKENS to emptyList<String>(),
            ).filterKeys { !existing.contains(it) }
            // A previous/ambiguous commit or another client may already have populated the profile.
            // Fill missing identity fields only; never replace tokens, preferences or existing names.
            if (fields.isNotEmpty()) transaction.set(ref, fields, SetOptions.merge())
        }.await()
    }

    override suspend fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): Unit =
        transitions.withLock {
            try {
                val previous = pendingRegistration?.takeIf {
                    it.submittedEmail == email && auth.currentUser?.uid == it.profile.uid
                }
                if (previous != null &&
                    (previous.profile.firstName != firstName || previous.profile.lastName != lastName)) {
                    // Retain the original attempt instead of creating another account or silently
                    // applying edited form fields to an already-created profile.
                    throw AuthException.RegistrationIncomplete()
                }
                val attempt = previous ?: withContext(NonCancellable) {
                    pendingRegistration = null
                    val created = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = created.user ?: throw AuthException.UidNotFound()
                    RegistrationAttempt(email, User(firstName, lastName, user.email ?: email, user.uid))
                        .also { pendingRegistration = it }
                }
                currentCoroutineContext().ensureActive()
                val user = auth.currentUser?.takeIf { it.uid == attempt.profile.uid }
                    ?: throw AuthException.InvalidCredentials()
                if (!attempt.verificationSent) {
                    withContext(NonCancellable) {
                        sendEmailVerification(user)
                        attempt.verificationSent = true
                    }
                }
                currentCoroutineContext().ensureActive()
                if (auth.currentUser?.uid != attempt.profile.uid) throw AuthException.InvalidCredentials()
                if (!attempt.profileSaved) {
                    saveUserFirestore(attempt.profile)
                    attempt.profileSaved = true
                }
                if (auth.currentUser?.uid != attempt.profile.uid) throw AuthException.InvalidCredentials()
                coordinator.prepare()
                queueToken()
                pendingRegistration = null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is FirebaseAuthUserCollisionException -> {
                        throw AuthException.EmailExists(e)
                    }

                    else -> {
                        val failure = e.toDataAccessFailure()
                        val pending = pendingRegistration
                        if (pending != null && pending.profile.uid == auth.currentUser?.uid &&
                            failure !is AuthException &&
                            failure !is DataAccessException.InvalidRemoteData) {
                            throw AuthException.RegistrationIncomplete(failure)
                        }
                        throw failure
                    }
                }
            }
        }

    private suspend fun sendEmailVerification(user: FirebaseUser) {
        try {
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw AuthException.VerificationEmailFailed(e)
        }
    }

    override suspend fun verifyUserAndSendResetEmail(
        email: String,
        firstName: String,
        lastName: String,
    ): Boolean {
        return try {
            if (lookup.resetIdentity(email, firstName, lastName)) {
                auth.sendPasswordResetEmail(email).await()
                true
            } else false
        } catch (e: Exception) {
            throw e.toDataAccessFailure()
        }
    }

    override suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        try {
            auth.confirmPasswordReset(oobCode, newPassword).await()
        } catch (e: Exception) {
            throw e.toPasswordResetFailure()
        }
    }

    override suspend fun signInWithGoogle(idToken: String?): Unit =
        transitions.withLock {
            pendingRegistration = null
            if (idToken.isNullOrBlank()) throw AuthException.InvalidCredentials()
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                completeAuth(auth.signInWithCredential(credential))
                coordinator.prepare()
                queueToken()
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthUserCollisionException) {
                throw AuthException.EmailExists(e)
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                throw AuthException.InvalidCredentials(e)
            } catch (e: Exception) {
                throw e.toDataAccessFailure()
            }
        }

    override suspend fun isUserRegistered(email: String): Boolean {
        return lookup.registered(email)
    }

    override suspend fun getUserName(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val document =
                firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
            if (auth.currentUser?.uid == uid) document.getString(UserFields.FIRST_NAME) else null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    override suspend fun signOut(): Unit =
        transitions.withLock {
            pendingRegistration = null
            // Bound remote token cleanup; a network outage must not prevent local sign-out.
            try {
                withTimeoutOrNull(2_000) { fcmTokenManager.removeFCMToken() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(
                    "AuthRepository",
                    "Token revocation remains pending (${e.javaClass.simpleName})",
                )
            }
            try {
                coordinator.signOut()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Cleanup may have detached the local account before failing, while Auth is still
                // signed in. Re-prepare that actual SDK state so the existing UI can safely retry.
                // Do not invent success, switch users or discard pending financial records.
                try {
                    coordinator.prepare()
                } catch (recovery: Exception) {
                    if (recovery is CancellationException) throw recovery
                    Log.w("AuthRepository", "Account recovery deferred (${recovery.javaClass.simpleName})")
                }
                throw e.toDataAccessFailure()
            }
            try {
                withTimeoutOrNull(2_000) { credentialSessionCleaner.clear() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(
                    "AuthRepository",
                    "Credential session cleanup failed (${e.javaClass.simpleName})",
                )
            }
            Unit
        }

    private suspend fun queueToken() {
        try {
            withTimeoutOrNull(2_000) { fcmTokenManager.updateFCMToken() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AuthRepository", "Token registration deferred (${e.javaClass.simpleName})")
        }
    }
}

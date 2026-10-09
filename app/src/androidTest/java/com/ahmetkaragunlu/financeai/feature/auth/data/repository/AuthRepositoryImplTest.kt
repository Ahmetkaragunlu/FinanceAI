package com.ahmetkaragunlu.financeai.feature.auth.data.repository

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.auth.data.local.session.CredentialSessionCleaner
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.AuthLookupRemote
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock

/** SDK Task doubles exercise the real repository, without creating accounts or sending email. */
class AuthRepositoryImplTest {
    private class Fixture {
        val auth = mock(FirebaseAuth::class.java)
        val firestore = mock(FirebaseFirestore::class.java)
        val coordinator = mock(SessionCoordinator::class.java)
        val tokens = mock(FCMTokenManager::class.java)
        val lookup = mock(AuthLookupRemote::class.java)
        var credentialFailure: Exception? = null
        var credentialClears = 0
        val repository = AuthRepositoryImpl(auth, firestore, coordinator, tokens, lookup,
            CredentialSessionCleaner { credentialClears++; credentialFailure?.let { throw it } })
        val created = mock(FirebaseUser::class.java)
        val result = mock(AuthResult::class.java)
        var current: FirebaseUser? = null
        var verified = false
        var createTask: Task<AuthResult> = Tasks.forResult(result)
        var verificationTask: Task<Void> = Tasks.forResult(null)
        var reloadTask: Task<Void> = Tasks.forResult(null)
        var profileFailure: Exception? = null
        var commitBeforeFailure = false
        var prepareFailure: Exception? = null
        var signOutFailure: Exception? = null
        var tokenFailure: Exception? = null
        val creates = AtomicInteger()
        val emails = AtomicInteger()
        val profiles = AtomicInteger()
        val events = CopyOnWriteArrayList<String>()
        val fields = mutableMapOf<String, Any?>()

        suspend fun setUp() {
            `when`(auth.currentUser).thenAnswer { current }
            `when`(created.uid).thenReturn("created-owner")
            `when`(created.email).thenReturn("user@example.com")
            `when`(created.isEmailVerified).thenAnswer { verified }
            `when`(result.user).thenReturn(created)
            doAnswer {
                creates.incrementAndGet(); events += "create"
                if (createTask.isSuccessful) current = created
                createTask
            }.`when`(auth).createUserWithEmailAndPassword(anyString(), anyString())
            doAnswer { emails.incrementAndGet(); events += "verify"; verificationTask }.`when`(created).sendEmailVerification()
            doAnswer { reloadTask }.`when`(created).reload()
            val users = mock(CollectionReference::class.java)
            val ref = mock(DocumentReference::class.java)
            val transaction = mock(Transaction::class.java)
            val document = mock(DocumentSnapshot::class.java)
            `when`(firestore.collection("users")).thenReturn(users)
            `when`(users.document("created-owner")).thenReturn(ref)
            `when`(transaction.get(ref)).thenReturn(document)
            `when`(document.contains(anyString())).thenAnswer { fields.containsKey(it.getArgument<String>(0)) }
            `when`(document.getString(anyString())).thenAnswer { fields[it.getArgument<String>(0)] as? String }
            var staged: Map<String, Any?> = emptyMap()
            doAnswer {
                staged = it.getArgument(1)
                transaction
            }.`when`(transaction).set(eq(ref), any(), any(SetOptions::class.java))
            doAnswer { invocation ->
                profiles.incrementAndGet(); events += "profile"; staged = emptyMap()
                try {
                    invocation.getArgument<Transaction.Function<Any?>>(0).apply(transaction)
                    if (profileFailure == null || commitBeforeFailure) fields.putAll(staged)
                    profileFailure?.let { Tasks.forException<Any?>(it) } ?: Tasks.forResult<Any?>(null)
                } catch (e: Exception) { Tasks.forException<Any?>(e) }
            }.`when`(firestore).runTransaction(any<Transaction.Function<Any?>>())
            doAnswer { events += "prepare"; prepareFailure?.let { throw it }; Unit }.`when`(coordinator).prepare()
            doAnswer { events += "logout"; signOutFailure?.let { throw it }; current = null; Unit }.`when`(coordinator).signOut()
            doAnswer { events += "token"; tokenFailure?.let { throw it }; Unit }.`when`(tokens).updateFCMToken()
            doAnswer { events += "revoke"; tokenFailure?.let { throw it }; Unit }.`when`(tokens).removeFCMToken()
            `when`(auth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(Tasks.forResult(result))
        }

        suspend fun register(email: String = "user@example.com", name: String = "First") =
            repository.registerUser(email, "synthetic-password", name, "Last")
    }

    private suspend fun failure(block: suspend () -> Unit): Exception {
        try { block() } catch (e: Exception) { return e }
        throw AssertionError("Expected failure")
    }

    @Test fun normalRegistrationKeepsSuccessfulStepOrderAndCompletedAccountIsNeverRewritten() = runBlocking {
        val f = Fixture().apply { setUp() }
        f.register()
        assertEquals(listOf("create", "verify", "profile", "prepare", "token"), f.events.toList())
        assertEquals("First", f.fields["firstName"])
        assertEquals(emptyList<String>(), f.fields["fcmTokens"])
        f.createTask = Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "diagnostic"))
        assertTrue(failure { f.register() } is AuthException.EmailExists)
        assertEquals(1, f.profiles.get())
        assertEquals(1, f.emails.get())
    }

    @Test fun failedVerificationRetriesTheCreatedUidWithoutAnotherSignup() = runBlocking {
        val f = Fixture().apply { setUp() }
        val network = FirebaseNetworkException("private")
        f.verificationTask = Tasks.forException(network)
        val failed = failure { f.register() }
        assertTrue(failed is AuthException.VerificationEmailFailed)
        assertSame(network, failed.cause)
        assertEquals(0, f.profiles.get())
        f.verificationTask = Tasks.forResult(null)
        f.register()
        assertEquals(1, f.creates.get())
        assertEquals(2, f.emails.get())
        assertEquals(1, f.profiles.get())
    }

    @Test fun cancellationAfterVerificationDoesNotResendItAndExternalAccountChangeCannotWriteProfile() = runBlocking {
        val f = Fixture().apply { setUp() }
        val pending = TaskCompletionSource<Void>()
        f.verificationTask = pending.task
        val registration = launch(start = CoroutineStart.UNDISPATCHED) { f.register() }
        registration.cancel()
        pending.setResult(null)
        withTimeout(5_000) { registration.join() }
        assertEquals(0, f.profiles.get())
        f.register()
        assertEquals(1, f.emails.get())
        assertEquals(1, f.profiles.get())

        val changed = Fixture().apply { setUp() }
        val verification = TaskCompletionSource<Void>()
        changed.verificationTask = verification.task
        val invalid = async(start = CoroutineStart.UNDISPATCHED) { failure { changed.register() } }
        changed.current = mock(FirebaseUser::class.java)
        verification.setResult(null)
        assertTrue(withTimeout(5_000) { invalid.await() } is AuthException.InvalidCredentials)
        assertEquals(0, changed.profiles.get())
    }

    @Test fun anotherAuthTransitionInvalidatesRetryAndMalformedStoredUidIsNotRepairedByOverwriting() = runBlocking {
        val f = Fixture().apply { setUp() }
        f.verificationTask = Tasks.forException(FirebaseNetworkException("private"))
        failure { f.register() }
        f.repository.signIn("user@example.com", "synthetic")
        f.createTask = Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "diagnostic"))
        assertTrue(failure { f.register() } is AuthException.EmailExists)
        assertEquals(0, f.profiles.get())
        val malformed = Fixture().apply { setUp(); fields["uid"] = "foreign-owner" }
        assertTrue(failure { malformed.register() } is DataAccessException.InvalidRemoteData)
        assertEquals(mapOf("uid" to "foreign-owner"), malformed.fields)
    }

    @Test fun failedProfileRetriesOnlyTheMissingStepAndPreservesConcurrentProfileAndPreferences() = runBlocking {
        val f = Fixture().apply { setUp() }
        f.profileFailure = FirebaseNetworkException("private")
        val incomplete = failure { f.register() }
        assertTrue(incomplete is AuthException.RegistrationIncomplete)
        assertTrue(incomplete.cause is DataAccessException.NetworkUnavailable)
        assertSame(f.profileFailure, incomplete.cause?.cause)
        f.fields.putAll(mapOf("firstName" to "Other device name", "currencyCode" to "EUR",
            "timeZoneId" to "Europe/Istanbul", "fcmTokens" to listOf("synthetic-token")))
        f.profileFailure = null
        f.register()
        assertEquals(1, f.creates.get())
        assertEquals(1, f.emails.get())
        assertEquals(2, f.profiles.get())
        assertEquals("Other device name", f.fields["firstName"])
        assertEquals("EUR", f.fields["currencyCode"])
        assertEquals("Europe/Istanbul", f.fields["timeZoneId"])
        assertEquals(listOf("synthetic-token"), f.fields["fcmTokens"])
    }

    @Test fun ambiguousProfileCommitIsIdempotentAndPreparationFailureDoesNotResendEmailOrProfile() = runBlocking {
        val f = Fixture().apply { setUp() }
        f.commitBeforeFailure = true
        f.profileFailure = FirebaseNetworkException("private")
        failure { f.register() }
        val committed = f.fields.toMap()
        f.profileFailure = null
        f.prepareFailure = FirebaseNetworkException("private")
        failure { f.register() }
        assertEquals(committed, f.fields)
        f.prepareFailure = null
        f.register()
        assertEquals(1, f.creates.get())
        assertEquals(1, f.emails.get())
        assertEquals(2, f.profiles.get())
    }

    @Test fun emailCollisionDifferentFormAccountSwitchAndNewRepositoryDoNotGrantRetryAuthority() = runBlocking {
        suspend fun partial(): Fixture = Fixture().apply {
            setUp(); verificationTask = Tasks.forException(FirebaseNetworkException("private"))
            failure { register() }
            verificationTask = Tasks.forResult(null)
            createTask = Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "diagnostic"))
        }
        val changedForm = partial()
        assertTrue(failure { changedForm.register(name = "Changed") } is AuthException.RegistrationIncomplete)
        assertEquals(0, changedForm.profiles.get())
        changedForm.register()
        assertEquals(1, changedForm.creates.get())
        assertEquals("First", changedForm.fields["firstName"])
        val switched = partial().apply { current = mock(FirebaseUser::class.java) }
        assertTrue(failure { switched.register() } is AuthException.EmailExists)
        assertEquals(0, switched.profiles.get())
        val restarted = partial()
        val newRepository = AuthRepositoryImpl(restarted.auth, restarted.firestore, restarted.coordinator,
            restarted.tokens, restarted.lookup, CredentialSessionCleaner {})
        assertTrue(failure { newRepository.registerUser("user@example.com", "synthetic", "First", "Last") } is AuthException.EmailExists)
        assertEquals(0, restarted.profiles.get())
    }

    @Test fun cancelledSdkCreationStillRecordsOnlyItsOwnUidBeforeReleasingTheTransitionLock() = runBlocking {
        val f = Fixture().apply { setUp() }
        val pending = TaskCompletionSource<AuthResult>()
        f.createTask = pending.task
        val registration = launch(start = CoroutineStart.UNDISPATCHED) { f.register() }
        registration.cancel()
        // The result is assigned to Auth by this controlled SDK boundary, not by the repository.
        f.current = f.created
        pending.setResult(f.result)
        withTimeout(5_000) { registration.join() }
        assertEquals(0, f.emails.get())
        f.register()
        assertEquals(1, f.creates.get())
        assertEquals(1, f.emails.get())
    }

    @Test fun signOutWaitsForOutstandingSdkTransitionAndInvalidatesAnyIncompleteRegistration() = runBlocking {
        val f = Fixture().apply { setUp() }
        val pending = TaskCompletionSource<AuthResult>()
        f.createTask = pending.task
        val registration = launch(start = CoroutineStart.UNDISPATCHED) { f.register() }
        registration.cancel()
        val signOut = async(start = CoroutineStart.UNDISPATCHED) { f.repository.signOut() }
        assertFalse(signOut.isCompleted)
        assertFalse(f.events.contains("logout"))
        f.current = f.created
        pending.setResult(f.result)
        withTimeout(5_000) { registration.join(); signOut.await() }
        assertNull(f.current)
        f.current = f.created
        f.createTask = Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "diagnostic"))
        assertTrue(failure { f.register() } is AuthException.EmailExists)
        assertEquals(0, f.profiles.get())
    }

    @Test fun verificationRefreshKeepsSignedOutUnverifiedVerifiedAndFailureResultsDistinct() = runBlocking {
        val f = Fixture().apply { setUp() }
        assertFalse(f.repository.refreshEmailVerification())
        f.current = f.created
        assertFalse(f.repository.refreshEmailVerification())
        assertTrue(f.events.isEmpty())
        f.verified = true
        assertTrue(f.repository.refreshEmailVerification())
        assertEquals(listOf("prepare"), f.events.toList())
        val denied = FirebaseFirestoreException("private", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        f.reloadTask = Tasks.forException(denied)
        val mapped = failure { f.repository.refreshEmailVerification() }
        assertTrue(mapped is DataAccessException.AccessDenied)
        assertSame(denied, mapped.cause)
        val cancelled = CancellationException()
        f.reloadTask = Tasks.forException(cancelled)
        assertSame(cancelled, failure { f.repository.refreshEmailVerification() })
        f.reloadTask = Tasks.forResult(null)
        val unknown = IllegalStateException("private")
        f.prepareFailure = unknown
        assertSame(unknown, failure { f.repository.refreshEmailVerification() })
        val network = FirebaseNetworkException("private")
        f.prepareFailure = network
        assertSame(network, failure { f.repository.refreshEmailVerification() }.cause)
    }

    @Test fun signInMapsCredentialsAndPreservesPreparationFailureAndCancellation() = runBlocking {
        val f = Fixture().apply { setUp() }
        val invalid = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "private")
        `when`(f.auth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(Tasks.forException(invalid))
        assertTrue(failure { f.repository.signIn("user@example.com", "synthetic") } is AuthException.InvalidCredentials)
        `when`(f.auth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(Tasks.forResult(f.result))
        f.current = f.created
        f.prepareFailure = FirebaseNetworkException("private")
        assertTrue(failure { f.repository.signIn("user@example.com", "synthetic") } is DataAccessException.NetworkUnavailable)
        val cancelled = CancellationException()
        f.prepareFailure = cancelled
        assertSame(cancelled, failure { f.repository.signIn("user@example.com", "synthetic") })
    }

    @Test fun googleSigninKeepsExistingSuccessfulFlowAndSpecificSdkFailureFamilies() = runBlocking {
        val f = Fixture().apply { setUp(); current = created }
        assertTrue(failure { f.repository.signInWithGoogle(null) } is AuthException.InvalidCredentials)
        assertTrue(f.events.isEmpty())
        `when`(f.auth.signInWithCredential(any(AuthCredential::class.java))).thenReturn(Tasks.forResult(f.result))
        f.repository.signInWithGoogle("synthetic-google-token")
        assertEquals(listOf("prepare", "token"), f.events.toList())
        val collision = FirebaseAuthUserCollisionException("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "private")
        `when`(f.auth.signInWithCredential(any(AuthCredential::class.java))).thenReturn(Tasks.forException(collision))
        val mapped = failure { f.repository.signInWithGoogle("synthetic-google-token") }
        assertTrue(mapped is AuthException.EmailExists)
        assertSame(collision, mapped.cause)
        val network = FirebaseNetworkException("private")
        `when`(f.auth.signInWithCredential(any(AuthCredential::class.java))).thenReturn(Tasks.forException(network))
        assertSame(network, failure { f.repository.signInWithGoogle("synthetic-google-token") }.cause)
    }

    @Test fun cancelledProfileCompletionIsNotSuccessAndRetryDoesNotDuplicateCommittedFieldsOrEmail() = runBlocking {
        val f = Fixture().apply { setUp() }
        val cancelled = CancellationException()
        f.commitBeforeFailure = true
        f.profileFailure = cancelled
        assertSame(cancelled, failure { f.register() })
        val committed = f.fields.toMap()
        f.profileFailure = null
        f.register()
        assertEquals(committed, f.fields)
        assertEquals(1, f.creates.get())
        assertEquals(1, f.emails.get())
    }

    @Test fun remoteCleanupFailureIsNonFatalButFailedLocalSignOutIsNotSuccessAndCanRetry() = runBlocking {
        val f = Fixture().apply { setUp(); current = created }
        f.tokenFailure = FirebaseNetworkException("private")
        f.credentialFailure = IllegalStateException("private")
        f.repository.signOut()
        assertNull(f.current)
        assertEquals(1, f.credentialClears)
        f.current = f.created
        val denied = FirebaseFirestoreException("private", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        f.signOutFailure = denied
        val failure = failure { f.repository.signOut() }
        assertTrue(failure is DataAccessException.AccessDenied)
        assertSame(denied, failure.cause)
        assertSame(f.created, f.current)
        assertEquals("prepare", f.events.last())
        assertEquals(1, f.credentialClears)
        f.signOutFailure = null
        f.repository.signOut()
        assertNull(f.current)
    }
}

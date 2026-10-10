package com.ahmetkaragunlu.financeai.feature.auth.data.repository

import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.ahmetkaragunlu.financeai.core.sync.testing.EmulatorAccountFixture
import com.ahmetkaragunlu.financeai.fcm.FCMTokenManager
import com.ahmetkaragunlu.financeai.feature.auth.data.remote.AuthLookupRemote
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

/** Real local Firestore transaction; Auth/email/token Tasks remain synthetic controlled boundaries. */
class AuthRegistrationProfileIntegrationTest {
    private suspend fun repository(f: EmulatorAccountFixture, owner: String): AuthRepositoryImpl {
        f.signIn(owner, verified = false)
        val user = checkNotNull(f.auth.currentUser)
        `when`(user.email).thenReturn("synthetic@example.com")
        `when`(user.sendEmailVerification()).thenReturn(Tasks.forResult(null))
        val result = mock(AuthResult::class.java)
        `when`(result.user).thenReturn(user)
        `when`(f.auth.createUserWithEmailAndPassword(anyString(), anyString())).thenReturn(Tasks.forResult(result))
        val coordinator = mock(SessionCoordinator::class.java)
        doAnswer { }.`when`(coordinator).prepare()
        val tokens = mock(FCMTokenManager::class.java)
        doAnswer { }.`when`(tokens).updateFCMToken()
        return AuthRepositoryImpl(f.auth, f.firestore, coordinator, tokens,
            mock(AuthLookupRemote::class.java)
        ) {}
    }

    @Test fun profileTransactionFillsMissingFieldsWithoutReplacingExistingIdentityTokensOrPreferences() = runBlocking {
        val f = EmulatorAccountFixture()
        try {
            val owner = "registration-${UUID.randomUUID()}"
            val repo = repository(f, owner)
            val profile = f.firestore.collection("users").document(owner)
            val existing = mapOf("uid" to owner, "email" to "synthetic@example.com", "firstName" to "Existing name",
                "fcmTokens" to listOf("synthetic-token"), "currencyCode" to "EUR", "timeZoneId" to "Europe/Istanbul")
            profile.set(existing).await()
            repo.registerUser("synthetic@example.com", "synthetic-password", "Proposed name", "Last")
            assertEquals(existing + ("lastName" to "Last"), profile.get().await().data)
        } finally { f.close() }
    }

    @Test fun malformedProfileUidRejectsRegistrationWithoutChangingTheDocument() = runBlocking {
        val f = EmulatorAccountFixture()
        try {
            val owner = "registration-${UUID.randomUUID()}"
            val repo = repository(f, owner)
            val profile = f.firestore.collection("users").document(owner)
            val existing = mapOf("uid" to "foreign-owner", "firstName" to "Preserved")
            profile.set(existing).await()
            try {
                repo.registerUser("synthetic@example.com", "synthetic-password", "Proposed", "Last")
                fail("Expected invalid remote identity")
            } catch (e: Exception) { assertTrue(e is DataAccessException.InvalidRemoteData) }
            assertEquals(existing, profile.get().await().data)
        } finally { f.close() }
    }
}

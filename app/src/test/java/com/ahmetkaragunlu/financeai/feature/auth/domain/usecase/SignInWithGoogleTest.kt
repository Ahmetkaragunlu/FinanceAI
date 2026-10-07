package com.ahmetkaragunlu.financeai.feature.auth.domain.usecase

import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.model.GoogleIdentity
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SignInWithGoogleTest {
    private class Repository : AuthRepository by FakeAuthRepository() {
        var registered = true
        var lookupCount = 0
        var token: String? = null
        var failure: Exception? = null
        override suspend fun isUserRegistered(email: String): Boolean { lookupCount++; return registered }
        override suspend fun signInWithGoogle(idToken: String?) { failure?.let { throw it }; token = idToken }
    }
    @Test fun unregisteredIdentityCannotCreateANewFirebaseAccount() = runTest {
        val repository = Repository().apply { registered = false }
        assertFalse(SignInWithGoogle(repository)(GoogleIdentity("user@example.test", "token")))
        assertNull(repository.token)
    }
    @Test fun registeredIdentityUsesTheSelectedToken() = runTest {
        val repository = Repository()
        assertTrue(SignInWithGoogle(repository)(GoogleIdentity("user@example.test", "selected-token")))
        assertEquals("selected-token", repository.token)
    }
    @Test fun missingTokenNeverStartsAnAuthTransition() = runTest {
        val repository = Repository()
        try { SignInWithGoogle(repository)(GoogleIdentity("user@example.test", "")); fail("Invalid credential accepted") }
        catch (_: AuthException.InvalidCredentials) { }
        assertEquals(0, repository.lookupCount)
    }
    @Test fun providerCollisionAndCancellationRemainFailuresWithoutFallbackSignUp() = runTest {
        val repository = Repository()
        repository.failure = AuthException.EmailExists()
        try { SignInWithGoogle(repository)(GoogleIdentity("user@example.test", "token")); fail("Collision hidden") }
        catch (_: AuthException.EmailExists) { }
        repository.failure = CancellationException()
        try { SignInWithGoogle(repository)(GoogleIdentity("user@example.test", "token")); fail("Cancellation hidden") }
        catch (_: CancellationException) { }
        assertNull(repository.token)
    }
}

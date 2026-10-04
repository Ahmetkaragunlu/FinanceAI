package com.ahmetkaragunlu.financeai.feature.auth.domain.usecase

import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInWithPasswordTest {
    @Test
    fun `verification is refreshed only after successful authentication`() = runBlocking {
        val repository = RecordingAuthRepository(verified = true)

        assertTrue(SignInWithPassword(repository)("user@example.com", "password"))
        assertEquals(listOf("signIn", "refreshVerification"), repository.calls)
        assertEquals("user@example.com" to "password", repository.credentials)
    }

    @Test
    fun `unverified account remains unverified`() = runBlocking {
        val repository = RecordingAuthRepository(verified = false)

        assertFalse(SignInWithPassword(repository)("user@example.com", "password"))
        assertEquals(listOf("signIn", "refreshVerification"), repository.calls)
    }

    @Test
    fun `authentication failure does not trigger verification refresh`() {
        val failure = AuthException.InvalidCredentials
        val repository = RecordingAuthRepository(signInFailure = failure)

        val thrown = assertThrows(AuthException::class.java) {
            runBlocking { SignInWithPassword(repository)("user@example.com", "wrong") }
        }

        assertSame(failure, thrown)
        assertEquals(listOf("signIn"), repository.calls)
    }

    @Test
    fun `refresh failure is not reported as verified login`() {
        val failure = IllegalStateException("refresh failed")
        val repository = RecordingAuthRepository(refreshFailure = failure)

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking { SignInWithPassword(repository)("user@example.com", "password") }
        }

        assertSame(failure, thrown)
        assertEquals(listOf("signIn", "refreshVerification"), repository.calls)
    }

    @Test
    fun `cancelled authentication is propagated without refreshing verification`() {
        val cancellation = CancellationException("cancelled")
        val repository = RecordingAuthRepository(signInFailure = cancellation)

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { SignInWithPassword(repository)("user@example.com", "password") }
        }

        assertSame(cancellation, thrown)
        assertEquals(listOf("signIn"), repository.calls)
    }

    private class RecordingAuthRepository(
        private val verified: Boolean = false,
        private val signInFailure: Exception? = null,
        private val refreshFailure: Exception? = null,
    ) : AuthRepository {
        val calls = mutableListOf<String>()
        var credentials: Pair<String, String>? = null

        override suspend fun signIn(email: String, password: String) {
            calls += "signIn"
            credentials = email to password
            signInFailure?.let { throw it }
        }

        override suspend fun refreshEmailVerification(): Boolean {
            calls += "refreshVerification"
            refreshFailure?.let { throw it }
            return verified
        }

        override suspend fun saveUser(email: String, password: String, firstName: String, lastName: String): Unit = unused()
        override suspend fun verifyUserAndSendResetEmail(email: String, firstName: String, lastName: String): Boolean = unused()
        override suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Unit = unused()
        override suspend fun signInWithGoogle(idToken: String?): Unit = unused()
        override suspend fun isUserRegistered(email: String): Boolean = unused()
        override suspend fun signOut(): Unit = unused()
        override suspend fun getUserName(): String? = unused()

        private fun unused(): Nothing = error("Unexpected repository call")
    }
}

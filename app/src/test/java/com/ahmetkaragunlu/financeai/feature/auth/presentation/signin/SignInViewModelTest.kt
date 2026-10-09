package com.ahmetkaragunlu.financeai.feature.auth.presentation.signin

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.model.GoogleIdentity
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithGoogle
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithPassword
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class Repository : AuthRepository by FakeAuthRepository() {
        var verified = true
        var registered = true
        var passwordCalls = 0
        var lookupCalls = 0
        var credentials: Pair<String, String>? = null
        var action: suspend () -> Unit = {}
        override suspend fun signIn(email: String, password: String) {
            passwordCalls++
            credentials = email to password
            action()
        }
        override suspend fun refreshEmailVerification() = verified
        override suspend fun isUserRegistered(email: String): Boolean { lookupCalls++; return registered }
        override suspend fun signInWithGoogle(idToken: String?) = action()
    }

    @Test fun `blank form never starts authentication and verification determines the result`() = runTest {
        val repository = Repository()
        val vm = SignInViewModel(SignInWithGoogle(repository), SignInWithPassword(repository))
        val store = ViewModelStore().apply { put("sign-in", vm) }
        try {
            vm.login()
            runCurrent()
            assertEquals(0, repository.passwordCalls)
            assertEquals(AuthState.FAILURE, vm.authState.value)
            vm.updateEmail("user@example.test")
            vm.updatePassword("password")
            vm.login()
            runCurrent()
            assertEquals(AuthState.SUCCESS, vm.authState.value)
            vm.resetAuthState()
            repository.verified = false
            vm.login()
            runCurrent()
            assertEquals(AuthState.EMAIL_NOT_VERIFIED, vm.authState.value)
        } finally { store.clear() }
    }

    @Test fun `pending password login shares its guard with Google and retains submitted credentials`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository().apply { action = { gate.await() } }
        val vm = SignInViewModel(SignInWithGoogle(repository), SignInWithPassword(repository))
        val store = ViewModelStore().apply { put("sign-in", vm) }
        try {
            vm.updateEmail("first@example.test")
            vm.updatePassword("first-password")
            vm.login()
            vm.updateEmail("changed@example.test")
            vm.login()
            vm.signInWithGoogle(GoogleIdentity("google@example.test", "synthetic-token"))
            runCurrent()
            assertEquals(1, repository.passwordCalls)
            assertEquals("first@example.test" to "first-password", repository.credentials)
            assertEquals(0, repository.lookupCalls)
            gate.complete(Unit)
            runCurrent()
            assertEquals(AuthState.SUCCESS, vm.authState.value)
        } finally { store.clear() }
    }

    @Test fun `Google registration and typed failure retain their distinct UI outcomes`() = runTest {
        val repository = Repository().apply { registered = false }
        val vm = SignInViewModel(SignInWithGoogle(repository), SignInWithPassword(repository))
        val store = ViewModelStore().apply { put("sign-in", vm) }
        try {
            val identity = GoogleIdentity("user@example.test", "synthetic-token")
            vm.signInWithGoogle(identity)
            runCurrent()
            assertEquals(AuthState.USER_NOT_FOUND, vm.authState.value)
            repository.registered = true
            repository.action = { throw DataAccessException.NetworkUnavailable() }
            vm.signInWithGoogle(identity)
            runCurrent()
            assertEquals(AuthState.FAILURE, vm.authState.value)
            assertEquals(R.string.error_network_unavailable, vm.failureMessageRes.value)
            vm.resetAuthState()
            assertEquals(AuthState.EMPTY, vm.authState.value)
            assertNull(vm.failureMessageRes.value)
        } finally { store.clear() }
    }

    @Test fun `failure and cancellation release the guard without inventing cancellation errors`() = runTest {
        val repository = Repository().apply { action = { throw AuthException.InvalidCredentials() } }
        val vm = SignInViewModel(SignInWithGoogle(repository), SignInWithPassword(repository))
        val store = ViewModelStore().apply { put("sign-in", vm) }
        try {
            vm.updateEmail("user@example.test")
            vm.updatePassword("password")
            vm.login()
            runCurrent()
            assertEquals(AuthState.INVALID_CREDENTIALS, vm.authState.value)
            vm.resetAuthState()
            repository.action = { throw CancellationException() }
            vm.login()
            runCurrent()
            assertEquals(AuthState.EMPTY, vm.authState.value)
            assertNull(vm.failureMessageRes.value)
            repository.action = {}
            vm.login()
            runCurrent()
            assertEquals(3, repository.passwordCalls)
            assertEquals(AuthState.SUCCESS, vm.authState.value)
        } finally { store.clear() }
    }
}

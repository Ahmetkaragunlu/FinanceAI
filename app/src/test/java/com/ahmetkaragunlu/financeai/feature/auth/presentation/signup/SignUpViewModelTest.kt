package com.ahmetkaragunlu.financeai.feature.auth.presentation.signup

import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `incomplete registration shows the retry explanation and keeps the original form`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = AuthException.RegistrationIncomplete() }
        val viewModel = SignUpViewModel(repository)
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password")
        viewModel.updateFirstName("First")
        viewModel.updateLastName("Last")
        assertTrue(viewModel.submitRegistration())
        advanceUntilIdle()
        assertEquals(AuthState.FAILURE, viewModel.authState.value)
        assertEquals(R.string.registration_incomplete_retry, viewModel.failureMessageRes.value)
        viewModel.resetAuthState()
        repository.registrationFailure = null
        assertTrue(viewModel.submitRegistration())
        advanceUntilIdle()
        assertEquals(2, repository.registrationCalls)
        assertEquals(FakeAuthRepository.Registration("user@example.com", "password", "First", "Last"), repository.registration)
        assertEquals(AuthState.VERIFICATION_EMAIL_SENT, viewModel.authState.value)
    }

    @Test
    fun `repeated submission is ignored while original form snapshot is registering`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { onRegistration = { gate.await() } }
        val viewModel = SignUpViewModel(repository)
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password")
        viewModel.updateFirstName("Ahmet")
        viewModel.updateLastName("Karagunlu")
        viewModel.registerUser()
        viewModel.updateEmail("changed@example.com")
        assertTrue(viewModel.submitRegistration())
        runCurrent()
        assertEquals(1, repository.registrationCalls)
        assertEquals("user@example.com", repository.registration?.email)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AuthState.VERIFICATION_EMAIL_SENT, viewModel.authState.value)
    }

    @Test
    fun `failed or cancelled registration releases the submission guard`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = AuthException.EmailExists() }
        val viewModel = SignUpViewModel(repository)
        viewModel.registerUser()
        advanceUntilIdle()
        repository.registrationFailure = CancellationException()
        viewModel.registerUser()
        advanceUntilIdle()
        repository.registrationFailure = null
        viewModel.registerUser()
        advanceUntilIdle()
        assertEquals(3, repository.registrationCalls)
        assertEquals(AuthState.VERIFICATION_EMAIL_SENT, viewModel.authState.value)
    }

    @Test
    fun `registration sends all form fields and keeps verification pending rather than logged in`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password")
        viewModel.updateFirstName("Ahmet")
        viewModel.updateLastName("Karagunlu")

        viewModel.registerUser()
        advanceUntilIdle()

        assertEquals(FakeAuthRepository.Registration("user@example.com", "password", "Ahmet", "Karagunlu"), repository.registration)
        assertEquals(AuthState.VERIFICATION_EMAIL_SENT, viewModel.authState.value)
    }

    @Test
    fun `existing account retains its specific result`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = AuthException.EmailExists() }
        val viewModel = SignUpViewModel(repository)
        viewModel.registerUser()
        advanceUntilIdle()
        assertEquals(AuthState.USER_ALREADY_EXISTS, viewModel.authState.value)
    }

    @Test
    fun `verification email failure is not treated as registration success`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = AuthException.VerificationEmailFailed() }
        val viewModel = SignUpViewModel(repository)
        viewModel.registerUser()
        advanceUntilIdle()
        assertEquals(AuthState.VERIFICATION_EMAIL_FAILED, viewModel.authState.value)
    }

    @Test
    fun `unexpected registration failure retains generic failure`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = IllegalStateException("failed") }
        val viewModel = SignUpViewModel(repository)
        viewModel.registerUser()
        advanceUntilIdle()
        assertEquals(AuthState.FAILURE, viewModel.authState.value)
    }
}

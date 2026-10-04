package com.ahmetkaragunlu.financeai.feature.auth.presentation.signup

import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `registration sends all form fields and keeps verification pending rather than logged in`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = SignUpViewModel(repository)
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password")
        viewModel.updateFirstName("Ahmet")
        viewModel.updateLastName("Karagunlu")

        viewModel.saveUser()
        advanceUntilIdle()

        assertEquals(FakeAuthRepository.Registration("user@example.com", "password", "Ahmet", "Karagunlu"), repository.registration)
        assertEquals(AuthState.VERIFICATION_EMAIL_SENT, viewModel.authState.value)
    }

    @Test
    fun `existing account retains its specific result`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = AuthException.EmailExists }
        val viewModel = SignUpViewModel(repository)
        viewModel.saveUser()
        advanceUntilIdle()
        assertEquals(AuthState.USER_ALREADY_EXISTS, viewModel.authState.value)
    }

    @Test
    fun `verification email failure is not treated as registration success`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = AuthException.VerificationEmailFailed }
        val viewModel = SignUpViewModel(repository)
        viewModel.saveUser()
        advanceUntilIdle()
        assertEquals(AuthState.VERIFICATION_EMAIL_FAILED, viewModel.authState.value)
    }

    @Test
    fun `unexpected registration failure retains generic failure`() = runTest {
        val repository = FakeAuthRepository().apply { registrationFailure = IllegalStateException("failed") }
        val viewModel = SignUpViewModel(repository)
        viewModel.saveUser()
        advanceUntilIdle()
        assertEquals(AuthState.FAILURE, viewModel.authState.value)
    }
}

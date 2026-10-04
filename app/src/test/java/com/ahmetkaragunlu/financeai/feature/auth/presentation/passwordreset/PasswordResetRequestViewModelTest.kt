package com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset

import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetRequestViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `matching user sends reset request with the same identity fields`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = PasswordResetRequestViewModel(repository)
        viewModel.updateEmail("user@example.com")
        viewModel.updateFirstName("Ahmet")
        viewModel.updateLastName("Karagunlu")
        viewModel.sendResetPasswordRequest()
        advanceUntilIdle()

        assertEquals(FakeAuthRepository.ResetRequest("user@example.com", "Ahmet", "Karagunlu"), repository.resetRequest)
        assertEquals(AuthState.SUCCESS, viewModel.authState.value)
    }

    @Test
    fun `nonmatching identity remains user not found`() = runTest {
        val repository = FakeAuthRepository().apply { resetRequestMatches = false }
        val viewModel = PasswordResetRequestViewModel(repository)
        viewModel.sendResetPasswordRequest()
        advanceUntilIdle()
        assertEquals(AuthState.USER_NOT_FOUND, viewModel.authState.value)
    }

    @Test
    fun `reset request failure is not reported as email sent`() = runTest {
        val repository = FakeAuthRepository().apply { resetRequestFailure = IllegalStateException("failed") }
        val viewModel = PasswordResetRequestViewModel(repository)
        viewModel.sendResetPasswordRequest()
        advanceUntilIdle()
        assertEquals(AuthState.FAILURE, viewModel.authState.value)
    }
}

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
class PasswordResetViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `password reset passes the link code and new password without losing either`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateNewPassword("new-password")
        viewModel.resetPassword("reset-code")
        advanceUntilIdle()
        assertEquals("reset-code" to "new-password", repository.resetConfirmation)
        assertEquals(AuthState.SUCCESS, viewModel.authState.value)
    }

    @Test
    fun `rejected reset is not reported as password changed`() = runTest {
        val repository = FakeAuthRepository().apply { resetConfirmationFailure = IllegalStateException("expired code") }
        val viewModel = PasswordResetViewModel(repository)
        viewModel.resetPassword("expired-code")
        advanceUntilIdle()
        assertEquals(AuthState.FAILURE, viewModel.authState.value)
    }
}

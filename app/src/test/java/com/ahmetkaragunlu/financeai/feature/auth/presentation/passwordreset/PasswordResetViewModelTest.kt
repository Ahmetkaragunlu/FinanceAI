package com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset

import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.testing.FakeAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun `repeated reset confirmation retains the original code and password`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { onResetConfirmation = { gate.await() } }
        val viewModel = PasswordResetViewModel(repository)
        viewModel.updateNewPassword("original-password")
        viewModel.updateConfirmPassword("original-password")
        assertTrue(viewModel.submitPasswordReset("original-code"))
        viewModel.updateNewPassword("changed-password")
        assertTrue(viewModel.submitPasswordReset("changed-code"))
        runCurrent()
        assertEquals(1, repository.resetConfirmationCalls)
        assertEquals("original-code" to "original-password", repository.resetConfirmation)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AuthState.SUCCESS, viewModel.authState.value)
    }

    @Test
    fun `failed or cancelled reset confirmation allows a new attempt`() = runTest {
        val repository = FakeAuthRepository().apply { resetConfirmationFailure = IllegalStateException("failed") }
        val viewModel = PasswordResetViewModel(repository)
        viewModel.resetPassword("code")
        advanceUntilIdle()
        repository.resetConfirmationFailure = CancellationException()
        viewModel.resetPassword("code")
        advanceUntilIdle()
        repository.resetConfirmationFailure = null
        viewModel.resetPassword("code")
        advanceUntilIdle()
        assertEquals(3, repository.resetConfirmationCalls)
        assertEquals(AuthState.SUCCESS, viewModel.authState.value)
    }

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

    @Test
    fun `field errors remain separate from password mismatch after helper renaming`() {
        val viewModel = PasswordResetViewModel(FakeAuthRepository())
        assertFalse(viewModel.shouldShowNewPasswordError())
        assertFalse(viewModel.shouldShowConfirmNewPasswordError())
        viewModel.updateNewPassword("abc")
        viewModel.updateConfirmPassword("abc")
        assertTrue(viewModel.shouldShowNewPasswordError())
        assertTrue(viewModel.shouldShowConfirmNewPasswordError())
        viewModel.updateNewPassword("valid-password")
        viewModel.updateConfirmPassword("another-valid-password")
        assertFalse(viewModel.shouldShowNewPasswordError())
        assertFalse(viewModel.shouldShowConfirmNewPasswordError())
        assertFalse(viewModel.checkPassword())
    }
}

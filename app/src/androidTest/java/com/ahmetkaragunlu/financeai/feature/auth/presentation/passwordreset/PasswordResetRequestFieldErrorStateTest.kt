package com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset

import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class PasswordResetRequestFieldErrorStateTest {
    @Test fun editingBlankInvalidAndValidFieldsKeepsTheExistingErrorVisibilityRules() {
        val viewModel = PasswordResetRequestViewModel(mock(AuthRepository::class.java))
        assertFalse(viewModel.shouldShowEmailError())
        assertFalse(viewModel.shouldShowFirstNameError())
        assertFalse(viewModel.shouldShowLastNameError())
        viewModel.updateEmail("invalid")
        viewModel.updateFirstName("Al")
        viewModel.updateLastName("A")
        assertTrue(viewModel.shouldShowEmailError())
        assertTrue(viewModel.shouldShowFirstNameError())
        assertTrue(viewModel.shouldShowLastNameError())
        viewModel.updateEmail("synthetic@example.test")
        viewModel.updateFirstName("Ahmet")
        viewModel.updateLastName("Kara")
        assertFalse(viewModel.shouldShowEmailError())
        assertFalse(viewModel.shouldShowFirstNameError())
        assertFalse(viewModel.shouldShowLastNameError())
    }
}

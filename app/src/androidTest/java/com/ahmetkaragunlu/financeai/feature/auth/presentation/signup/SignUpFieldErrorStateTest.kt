package com.ahmetkaragunlu.financeai.feature.auth.presentation.signup

import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class SignUpFieldErrorStateTest {
    @Test fun editingBlankInvalidAndValidFieldsKeepsTheExistingErrorVisibilityRules() {
        val viewModel = SignUpViewModel(mock(AuthRepository::class.java))
        assertFalse(viewModel.shouldShowEmailError())
        assertFalse(viewModel.shouldShowPasswordError())
        assertFalse(viewModel.shouldShowFirstNameError())
        assertFalse(viewModel.shouldShowLastNameError())
        viewModel.updateEmail("invalid")
        viewModel.updatePassword("abc")
        viewModel.updateFirstName("Al")
        viewModel.updateLastName("A")
        assertTrue(viewModel.shouldShowEmailError())
        assertTrue(viewModel.shouldShowPasswordError())
        assertTrue(viewModel.shouldShowFirstNameError())
        assertTrue(viewModel.shouldShowLastNameError())
        viewModel.updateEmail("synthetic@example.test")
        viewModel.updatePassword("valid-password")
        viewModel.updateFirstName("Ahmet")
        viewModel.updateLastName("Kara")
        assertFalse(viewModel.shouldShowEmailError())
        assertFalse(viewModel.shouldShowPasswordError())
        assertFalse(viewModel.shouldShowFirstNameError())
        assertFalse(viewModel.shouldShowLastNameError())
    }
}

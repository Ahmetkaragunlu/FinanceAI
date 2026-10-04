package com.ahmetkaragunlu.financeai.feature.auth.presentation.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFormValidationTest {
    @Test
    fun `password threshold and blank rejection remain shared across registration and reset`() {
        assertFalse(AuthFormValidation.isPasswordValid("12345"))
        assertFalse(AuthFormValidation.isPasswordValid("      "))
        assertTrue(AuthFormValidation.isPasswordValid("123456"))
    }

    @Test
    fun `every first name word must keep the existing minimum length`() {
        assertFalse(AuthFormValidation.isFirstNameValid(""))
        assertFalse(AuthFormValidation.isFirstNameValid("Ali V"))
        assertTrue(AuthFormValidation.isFirstNameValid("  Ali Can  "))
    }

    @Test
    fun `last name keeps the existing blank and length rules without new normalization`() {
        assertFalse(AuthFormValidation.isLastNameValid("  "))
        assertFalse(AuthFormValidation.isLastNameValid("A"))
        assertTrue(AuthFormValidation.isLastNameValid("Ab"))
        assertTrue(AuthFormValidation.isLastNameValid(" A"))
    }
}

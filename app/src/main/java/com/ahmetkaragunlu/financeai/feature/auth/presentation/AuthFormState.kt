package com.ahmetkaragunlu.financeai.feature.auth.presentation

data class RegistrationFormState(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val emailError: Boolean,
    val passwordError: Boolean,
    val firstNameError: Boolean,
    val lastNameError: Boolean,
)

data class ResetRequestFormState(
    val email: String,
    val firstName: String,
    val lastName: String,
    val emailError: Boolean,
    val firstNameError: Boolean,
    val lastNameError: Boolean,
)

data class ResetPasswordFormState(
    val password: String,
    val confirmation: String,
    val passwordError: Boolean,
    val confirmationError: Boolean,
)

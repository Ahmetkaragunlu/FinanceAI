package com.ahmetkaragunlu.financeai.feature.auth.presentation.validation

import androidx.core.util.PatternsCompat

/** Shared existing form rules; no registration or password-reset policy changes. */
internal object AuthFormValidation {
    fun isEmailValid(email: String): Boolean = PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
    fun isPasswordValid(password: String): Boolean = password.isNotBlank() && password.length >= 6
    fun isFirstNameValid(firstName: String): Boolean =
        firstName.trim().split("\\s+".toRegex()).all { it.length >= 3 }
    fun isLastNameValid(lastName: String): Boolean = lastName.isNotBlank() && lastName.length >= 2
}

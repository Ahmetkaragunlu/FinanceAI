package com.ahmetkaragunlu.financeai.feature.auth.presentation.mapper

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.error.dataErrorMessageRes
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException

@StringRes
fun authErrorMessageRes(error: Throwable): Int =
    when (error) {
        is AuthException.EmailExists -> R.string.this_email_is_already_exists
        is AuthException.InvalidCredentials -> R.string.invalid_email_or_password
        is AuthException.UidNotFound -> R.string.user_not_found
        is AuthException.VerificationEmailFailed -> R.string.email_verification_could_not_be_sent
        is AuthException.ExpiredResetCode -> R.string.password_reset_link_expired
        is AuthException.InvalidResetCode -> R.string.password_reset_link_invalid
        else -> dataErrorMessageRes(error) ?: R.string.something_went_wrong
    }

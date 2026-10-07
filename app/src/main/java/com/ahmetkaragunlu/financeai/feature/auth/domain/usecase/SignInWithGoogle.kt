package com.ahmetkaragunlu.financeai.feature.auth.domain.usecase

import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.model.GoogleIdentity
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(identity: GoogleIdentity): Boolean {
        if (identity.email.isBlank()) throw AuthException.MissingGoogleEmail()
        if (identity.idToken.isBlank()) throw AuthException.InvalidCredentials()
        if (!repository.isUserRegistered(identity.email)) return false
        repository.signInWithGoogle(identity.idToken)
        return true
    }
}

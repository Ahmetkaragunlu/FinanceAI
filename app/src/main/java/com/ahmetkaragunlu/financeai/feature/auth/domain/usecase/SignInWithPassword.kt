package com.ahmetkaragunlu.financeai.feature.auth.domain.usecase

import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Signs in before refreshing verification, preserving the existing login sequence. */
class SignInWithPassword @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Boolean {
        authRepository.signIn(email, password)
        return authRepository.refreshEmailVerification()
    }
}

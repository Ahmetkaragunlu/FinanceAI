package com.ahmetkaragunlu.financeai.feature.auth.navigation

import kotlinx.serialization.Serializable

@Serializable data object SignInDestination

@Serializable data object SignUpDestination

@Serializable data object PasswordResetRequestDestination

@Serializable data class PasswordResetDestination(val oobCode: String? = null)

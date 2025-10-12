package com.ahmetkaragunlu.financeai.screens.auth

sealed class AuthException(message: String? = null): Exception(message) {
    object EmailExists : AuthException("This email is already registered")
    object UidNotFound : AuthException ("Uid not found")
    object VerificationEmailFailed : AuthException("Failed to send verification email")
    object InvalidCredentials : AuthException("Invalid email or password")


}
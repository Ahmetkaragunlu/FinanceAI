package com.ahmetkaragunlu.financeai.screens.auth

sealed class AuthException(message: String? = null): Exception(message) {
    object NameExists : AuthException("This name already exists")
    object EmailExists : AuthException("This email is already registered")
    object UidNotFound : AuthException ("Uid not found")
    object UserNotRegistered : AuthException ("User not registered")
    object IdTokenIsNull : AuthException("Id token is null")
    object InvalidOobCode : AuthException("The password reset link is invalid or expired")
    class Unknown(message: String? = "Unknown error") : AuthException(message)

}
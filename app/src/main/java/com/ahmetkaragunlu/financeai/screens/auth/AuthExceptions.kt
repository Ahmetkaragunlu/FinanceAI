package com.ahmetkaragunlu.financeai.screens.auth

sealed class AuthException(message: String? = null): Exception(message) {
    object NameExists : AuthException("This name already exists")
    object EmailExists : AuthException("This email is already registered")
    object UidNotFound : AuthException ("Uid not found")
    class Unknown(message: String? = "Unknown error") : AuthException(message)

}
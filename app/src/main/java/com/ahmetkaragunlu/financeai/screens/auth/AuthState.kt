package com.ahmetkaragunlu.financeai.screens.auth

enum class AuthState {
    EMPTY,
    SUCCESS,
    FAILURE,
    USER_ALREADY_EXISTS,
    VERIFICATION_EMAIL_SENT,
    VERIFICATION_EMAIL_FAILED,
    EMAIL_NOT_VERIFIED,
    INVALID_CREDENTIALS,
    USER_NOT_FOUND
}
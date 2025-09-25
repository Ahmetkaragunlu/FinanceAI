package com.ahmetkaragunlu.financeai.screens.auth

enum class AuthState {
    EMPTY,
    SUCCESS,
    FAILURE,
    USER_ALREADY_EXISTS,
    USER_NAME_EXISTS,
    INVALID_CREDENTIALS,
    INVALID_EMAIL_OR_PASSWORD,
    USER_NOT_REGISTERED,
    ID_TOKEN_IS_NULL
}
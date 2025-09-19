package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.firebaseRepo.AuthRepository
import com.ahmetkaragunlu.financeai.firebaseRepo.AuthRepositoryImpl
import com.ahmetkaragunlu.financeai.screens.auth.AuthException
import com.ahmetkaragunlu.financeai.screens.auth.AuthState
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signUp(email: String, firsName: String, lastName: String, password: String) {
        viewModelScope.launch {
            _authState.value = try {
                authRepository.saveUser(
                    email = email,
                    firstName = firsName,
                    lastName = lastName,
                    password = password
                )
                AuthState.SUCCESS
            } catch (e: Exception) {
                when (e) {
                    is AuthException.NameExists -> AuthState.USER_NAME_EXISTS
                    is AuthException.EmailExists -> AuthState.USER_ALREADY_EXISTS
                    is AuthException.UidNotFound -> AuthState.FAILURE
                    else -> AuthState.FAILURE
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = try {
                authRepository.signIn(email = email, password = password)
                AuthState.SUCCESS
            } catch (e: Exception) {
                when (e) {
                    is FirebaseAuthInvalidUserException -> AuthState.INVALID_CREDENTIALS
                    is FirebaseAuthInvalidCredentialsException -> AuthState.INVALID_EMAIL_OR_PASSWORD
                    else -> AuthState.FAILURE
                }
            }
        }
    }

    fun logOut() {
        authRepository.logOut()
        _authState.value = AuthState.EMPTY
    }
}
package com.ahmetkaragunlu.financeai.viewmodel

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.firebaseRepo.AuthRepository
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


    fun signUp(email: String, firstName: String, lastName: String, password: String) {
        viewModelScope.launch {
            _authState.value = try {
                authRepository.saveUser(
                    email = email,
                    firstName = firstName,
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
        viewModelScope.launch {
            authRepository.logOut()
            _authState.value = AuthState.EMPTY
        }
    }
    fun login() {
        if(inputEmail.isBlank() || inputPassword.isBlank()) {
            _authState.value = AuthState.FAILURE
            return
        }
            signIn(email = inputEmail, password = inputPassword)
    }

    fun saveUser() {
            signUp(
                email = inputEmail,
                firstName = inputFirstName,
                lastName = inputLastName,
                password = inputPassword
            )
    }
    fun resetAuthState() {
        _authState.value = AuthState.EMPTY
    }


    var inputFirstName by mutableStateOf("")
        private set
    var inputEmail by mutableStateOf("")
        private set
    var inputPassword by mutableStateOf("")
        private set
    var inputLastName by mutableStateOf("")
        private set
    var passwordVisibility by mutableStateOf(false)

    fun updateFirstName(firstName: String) {
        inputFirstName = firstName
    }

    fun updateLastName(lastName: String) {
        inputLastName = lastName
    }

    fun updatePassword(password: String) {
        inputPassword = password
    }

    fun updateEmail(email: String) {
        inputEmail = email
    }

    fun isEmailValid() = Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()
    fun isValidPassword() = inputPassword.isNotBlank() && inputPassword.length >= 6
    fun isValidFirstName() = inputFirstName.trim().split("\\s+".toRegex()).all { it.length >= 3 }
    fun isValidLastName() = inputLastName.length >= 2 && inputLastName.isNotBlank()

    fun emailSupportingText() = !isEmailValid() && inputEmail.isNotBlank()
    fun passwordSupportingText() = !isValidPassword() && inputPassword.isNotBlank()
    fun firstNameSupportingText() = !isValidFirstName() && inputFirstName.isNotBlank()
    fun lastNameSupportingText() = !isValidLastName() && inputLastName.isNotBlank()

    fun isValid() = isValidPassword() && isValidLastName() && isValidFirstName() && isEmailValid()


}
package com.ahmetkaragunlu.financeai.feature.auth.presentation.signup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.mapper.authErrorMessageRes
import com.ahmetkaragunlu.financeai.feature.auth.presentation.validation.AuthFormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel @Inject constructor(private val authRepository: AuthRepository) :
    ViewModel() {
    fun submitRegistration(): Boolean {
        if (!isValidUser()) return false
        saveUser()
        return true
    }

    private val mutableFailureMessage = MutableStateFlow<Int?>(null)
    val failureMessageRes = mutableFailureMessage.asStateFlow()
    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var inputFirstName by mutableStateOf("")
        private set

    var inputLastName by mutableStateOf("")
        private set

    var inputEmail by mutableStateOf("")
        private set

    var inputPassword by mutableStateOf("")
        private set

    private fun signUp(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value =
                try {
                    authRepository.saveUser(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName,
                    )
                    AuthState.VERIFICATION_EMAIL_SENT
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    mutableFailureMessage.value = authErrorMessageRes(e)
                    when (e) {
                        is AuthException.EmailExists -> AuthState.USER_ALREADY_EXISTS
                        is AuthException.VerificationEmailFailed ->
                            AuthState.VERIFICATION_EMAIL_FAILED
                        else -> AuthState.FAILURE
                    }
                }
        }
    }

    fun saveUser() {
        signUp(
            email = inputEmail,
            firstName = inputFirstName,
            lastName = inputLastName,
            password = inputPassword,
        )
    }

    fun resetAuthState() {
        mutableFailureMessage.value = null
        _authState.value = AuthState.EMPTY
    }

    fun updateFirstName(firstName: String) {
        inputFirstName = firstName
    }

    fun updateLastName(lastName: String) {
        inputLastName = lastName
    }

    fun updateEmail(email: String) {
        inputEmail = email
    }

    fun updatePassword(password: String) {
        inputPassword = password
    }

    fun isEmailValid() = AuthFormValidation.isEmailValid(inputEmail)

    fun isValidPassword() = AuthFormValidation.isPasswordValid(inputPassword)

    fun isValidFirstName() = AuthFormValidation.isFirstNameValid(inputFirstName)

    fun isValidLastName() = AuthFormValidation.isLastNameValid(inputLastName)

    fun emailSupportingText() = !isEmailValid() && inputEmail.isNotBlank()

    fun passwordSupportingText() = !isValidPassword() && inputPassword.isNotBlank()

    fun firstNameSupportingText() = !isValidFirstName() && inputFirstName.isNotBlank()

    fun lastNameSupportingText() = !isValidLastName() && inputLastName.isNotBlank()

    fun isValidUser() =
        isValidPassword() && isValidLastName() && isValidFirstName() && isEmailValid()
}

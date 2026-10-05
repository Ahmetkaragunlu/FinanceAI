package com.ahmetkaragunlu.financeai.feature.auth.presentation.passwordreset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.validation.AuthFormValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PasswordResetRequestViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var inputFirstName by mutableStateOf("")
        private set
    var inputLastName by mutableStateOf("")
        private set
    var inputEmail by mutableStateOf("")
        private set
    var showDialog by mutableStateOf(false)

    fun sendResetPasswordRequest() {
        viewModelScope.launch {
            try {
                val result = authRepository.verifyUserAndSendResetEmail(
                    inputEmail,
                    inputFirstName,
                    inputLastName
                )
                _authState.value = if (result) AuthState.SUCCESS else AuthState.USER_NOT_FOUND
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _authState.value = AuthState.FAILURE
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.EMPTY
    }

    fun updateFirstName(firstName: String) { inputFirstName = firstName }

    fun updateLastName(lastName: String) { inputLastName = lastName }

    fun updateEmail(email: String) { inputEmail = email }

    fun isEmailValid() = AuthFormValidation.isEmailValid(inputEmail)

    fun isValidFirstName() = AuthFormValidation.isFirstNameValid(inputFirstName)

    fun isValidLastName() = AuthFormValidation.isLastNameValid(inputLastName)

    fun emailSupportingText() = !isEmailValid() && inputEmail.isNotBlank()

    fun firstNameSupportingText() = !isValidFirstName() && inputFirstName.isNotBlank()

    fun lastNameSupportingText() = !isValidLastName() && inputLastName.isNotBlank()

    fun isValidResetRequestPassword() = isValidLastName() && isValidFirstName() && isEmailValid()
}

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var inputNewPassword by mutableStateOf("")
        private set
    var inputConfirmPassword by mutableStateOf("")
        private set
    var passwordVisibility by mutableStateOf(false)
    var confirmPasswordVisibility by mutableStateOf(false)
    var showDialog by mutableStateOf(false)

    fun resetPassword(oobCode: String) {
        viewModelScope.launch {
            try {
                authRepository.confirmPasswordReset(oobCode, inputNewPassword)
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _authState.value = AuthState.FAILURE
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.EMPTY
    }

    fun updateNewPassword(newPassword: String) { inputNewPassword = newPassword }

    fun updateConfirmPassword(confirmPassword: String) { inputConfirmPassword = confirmPassword }

    fun checkPassword() = inputNewPassword == inputConfirmPassword

    fun isValidNewPassword() = AuthFormValidation.isPasswordValid(inputNewPassword)

    fun isValidConfirmNewPassword() = AuthFormValidation.isPasswordValid(inputConfirmPassword)

    fun newPasswordSupportingText() = !isValidNewPassword() && inputNewPassword.isNotBlank()

    fun confirmNewPasswordSupportingText() = !isValidConfirmNewPassword() && inputConfirmPassword.isNotBlank()

    fun isValidResetPassword() = isValidNewPassword() && isValidConfirmNewPassword()
}

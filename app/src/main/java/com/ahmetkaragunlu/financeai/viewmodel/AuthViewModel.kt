package com.ahmetkaragunlu.financeai.viewmodel

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.firebaserepo.AuthRepository
import com.ahmetkaragunlu.financeai.screens.auth.AuthException
import com.ahmetkaragunlu.financeai.screens.auth.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    fun signUp(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value = try {
                authRepository.saveUser(email = email,password=password, firstName = firstName, lastName = lastName)
                AuthState.VERIFICATION_EMAIL_SENT
            } catch (e: Exception) {
                when (e) {
                    is AuthException.EmailExists -> AuthState.USER_ALREADY_EXISTS
                    is AuthException.VerificationEmailFailed -> AuthState.VERIFICATION_EMAIL_FAILED
                    else -> AuthState.FAILURE
                }
            }
        }
    }
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = try {
                authRepository.signIn(email, password)
                auth.currentUser?.reload()?.await()

                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    AuthState.SUCCESS
                } else {
                    AuthState.EMAIL_NOT_VERIFIED
                }
            } catch (e: Exception) {
                when (e) {
                    is AuthException.InvalidCredentials -> AuthState.INVALID_CREDENTIALS
                    else -> AuthState.FAILURE
                }
            }
        }
    }
    fun login() {
        if (inputEmail.isBlank() || inputPassword.isBlank()) {
            _authState.value = AuthState.FAILURE
            return
        }
        signIn(email = inputEmail, password = inputPassword)
    }
    fun clearSignInFields() {
        inputEmail = ""
        inputPassword = ""
    }
    fun saveUser() {
        signUp(
            email = inputEmail,
            firstName = inputFirstName,
            lastName = inputLastName,
            password = inputPassword
        )
    }
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
                _authState.value = AuthState.FAILURE
            }
        }
    }
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
    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _authState.value = try {
                googleSignInClient.signOut().await()
                val email = account.email ?: throw Exception("Email not found")

                val isRegistered = authRepository.isUserRegistered(email)
                if (isRegistered) {
                    authRepository.signInWithGoogle(account)
                    AuthState.SUCCESS
                } else {
                    AuthState.USER_NOT_FOUND
                }
            } catch (e: Exception) {
                AuthState.FAILURE
            }
        }
    }
    fun getGoogleSignInIntent() = googleSignInClient.signInIntent
    fun resetAuthState() {
        _authState.value = AuthState.EMPTY
    }

    //  UI Input State Variables
    var inputFirstName by mutableStateOf("")
        private set
    var inputLastName by mutableStateOf("")
        private set
    var inputEmail by mutableStateOf("")
        private set
    var inputPassword by mutableStateOf("")
        private set
    var inputNewPassword by mutableStateOf("")
        private set
    var inputConfirmPassword by mutableStateOf("")
        private set


    //  UI Helper Variables (password visibility, dialog states)
    var passwordVisibility by mutableStateOf(false)
    var confirmPasswordVisibility by mutableStateOf(false)
    var showDialog by mutableStateOf(false)


    //  Update Input Fields
    fun updateFirstName(firstName: String) { inputFirstName = firstName }
    fun updateLastName(lastName: String) { inputLastName = lastName }
    fun updateEmail(email: String) { inputEmail = email }
    fun updatePassword(password: String) { inputPassword = password }
    fun updateNewPassword(newPassword: String) { inputNewPassword = newPassword }
    fun updateConfirmPassword(confirmPassword: String) { inputConfirmPassword = confirmPassword }

    // Validation Functions (Form Validation)
    fun checkPassword() = inputNewPassword == inputConfirmPassword
    fun isValidNewPassword() = inputNewPassword.isNotBlank() && inputNewPassword.length >= 6
    fun isValidConfirmNewPassword() = inputConfirmPassword.isNotBlank() && inputConfirmPassword.length >= 6
    fun isEmailValid() = Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()
    fun isValidPassword() = inputPassword.isNotBlank() && inputPassword.length >= 6
    fun isValidFirstName() = inputFirstName.trim().split("\\s+".toRegex()).all { it.length >= 3 }
    fun isValidLastName() = inputLastName.isNotBlank() && inputLastName.length >= 2

    //  Supporting Text States (For UI error messages)
    fun emailSupportingText() = !isEmailValid() && inputEmail.isNotBlank()
    fun passwordSupportingText() = !isValidPassword() && inputPassword.isNotBlank()
    fun firstNameSupportingText() = !isValidFirstName() && inputFirstName.isNotBlank()
    fun lastNameSupportingText() = !isValidLastName() && inputLastName.isNotBlank()
    fun newPasswordSupportingText() = !isValidNewPassword() && inputNewPassword.isNotBlank()
    fun confirmNewPasswordSupportingText() = !isValidConfirmNewPassword() && inputConfirmPassword.isNotBlank()

    // General Form Validations (Used to enable/disable buttons)
    fun isValidUser() = isValidPassword() && isValidLastName() && isValidFirstName() && isEmailValid()
    fun isValidResetPassword() = isValidNewPassword() && isValidConfirmNewPassword()
    fun isValidResetRequestPassword() = isValidLastName() && isValidFirstName() && isEmailValid()
}

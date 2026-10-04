package com.ahmetkaragunlu.financeai.feature.auth.presentation.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithPassword
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInClient: GoogleSignInClient,
    private val signInWithPassword: SignInWithPassword
) : ViewModel() {
    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var inputEmail by mutableStateOf("")
        private set
    var inputPassword by mutableStateOf("")
        private set
    var passwordVisibility by mutableStateOf(false)

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = try {
                if (signInWithPassword(email, password)) {
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

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _authState.value = try {
                googleSignInClient.signOut().await()
                val email = account.email ?: throw Exception("Email not found")

                val isRegistered = authRepository.isUserRegistered(email)
                if (isRegistered) {
                    authRepository.signInWithGoogle(account.idToken)
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

    fun updateEmail(email: String) { inputEmail = email }

    fun updatePassword(password: String) { inputPassword = password }
}

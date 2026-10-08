package com.ahmetkaragunlu.financeai.feature.auth.presentation.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.auth.domain.error.AuthException
import com.ahmetkaragunlu.financeai.feature.auth.domain.model.GoogleIdentity
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithGoogle
import com.ahmetkaragunlu.financeai.feature.auth.domain.usecase.SignInWithPassword
import com.ahmetkaragunlu.financeai.feature.auth.presentation.AuthState
import com.ahmetkaragunlu.financeai.feature.auth.presentation.mapper.authErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SignInViewModel
@Inject
constructor(
    private val googleSignIn: SignInWithGoogle,
    private val signInWithPassword: SignInWithPassword,
) : ViewModel() {
    private val mutableFailureMessage = MutableStateFlow<Int?>(null)
    val failureMessageRes = mutableFailureMessage.asStateFlow()
    private val _authState = MutableStateFlow(AuthState.EMPTY)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var inputEmail by mutableStateOf("")
        private set

    var inputPassword by mutableStateOf("")
        private set

    private var signingIn = false

    private fun signIn(email: String, password: String) {
        if (signingIn) return
        signingIn = true
        viewModelScope.launch {
            try {
                _authState.value =
                    if (signInWithPassword(email, password)) {
                        AuthState.SUCCESS
                    } else {
                        AuthState.EMAIL_NOT_VERIFIED
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutableFailureMessage.value = authErrorMessageRes(e)
                _authState.value =
                    when (e) {
                        is AuthException.InvalidCredentials -> AuthState.INVALID_CREDENTIALS
                        else -> AuthState.FAILURE
                    }
            } finally {
                signingIn = false
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

    fun signInWithGoogle(identity: GoogleIdentity) {
        if (signingIn) return
        signingIn = true
        viewModelScope.launch {
            try {
                _authState.value =
                    if (googleSignIn(identity)) AuthState.SUCCESS else AuthState.USER_NOT_FOUND
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutableFailureMessage.value = authErrorMessageRes(e)
                _authState.value = AuthState.FAILURE
            } finally {
                signingIn = false
            }
        }
    }

    fun resetAuthState() {
        mutableFailureMessage.value = null
        _authState.value = AuthState.EMPTY
    }

    fun updateEmail(email: String) {
        inputEmail = email
    }

    fun updatePassword(password: String) {
        inputPassword = password
    }
}

package com.ahmetkaragunlu.financeai.feature.auth.presentation.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.auth.presentation.mapper.authErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(private val authRepository: AuthRepository) :
    ViewModel() {
    var signOutComplete by mutableStateOf(false)
        private set

    private var signingOut = false

    var failureMessageRes by mutableStateOf<Int?>(null)
        private set

    fun consumeFailure() {
        failureMessageRes = null
    }

    fun consumeSignOutResult() {
        signOutComplete = false
    }

    fun performSignOut() {
        if (signingOut || signOutComplete) return
        signingOut = true
        failureMessageRes = null
        viewModelScope.launch {
            try {
                authRepository.signOut()
                currentCoroutineContext().ensureActive()
                signOutComplete = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failureMessageRes = authErrorMessageRes(e)
            } finally {
                signingOut = false
            }
        }
    }
}

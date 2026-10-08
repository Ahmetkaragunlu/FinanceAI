package com.ahmetkaragunlu.financeai.feature.auth.presentation.session

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
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

    fun consumeSignOutResult() {
        signOutComplete = false
    }

    fun performSignOut() {
        if (signingOut || signOutComplete) return
        signingOut = true
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("SessionViewModel", "Sign-out failed (${e.javaClass.simpleName})")
            } finally {
                signingOut = false
                currentCoroutineContext().ensureActive()
                signOutComplete = true
            }
        }
    }
}

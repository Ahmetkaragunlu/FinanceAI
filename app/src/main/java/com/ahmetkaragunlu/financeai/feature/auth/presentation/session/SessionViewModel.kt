package com.ahmetkaragunlu.financeai.feature.auth.presentation.session

import android.util.Log
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
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun performSignOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("SessionViewModel", "Sign-out failed (${e.javaClass.simpleName})")
            } finally {
                currentCoroutineContext().ensureActive()
                onSignOutComplete()
            }
        }
    }
}

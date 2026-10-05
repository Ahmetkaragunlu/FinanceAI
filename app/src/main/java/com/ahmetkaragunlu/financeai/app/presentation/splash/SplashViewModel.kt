package com.ahmetkaragunlu.financeai.app.presentation.splash

import androidx.lifecycle.ViewModel
import com.ahmetkaragunlu.financeai.core.session.SessionCoordinator
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val coordinator: SessionCoordinator,
    private val auth: FirebaseAuth
) : ViewModel() {
    suspend fun canOpenFinance(): Boolean {
        if (auth.currentUser?.isEmailVerified != true) return false
        coordinator.prepare()
        return coordinator.session.account.value?.ownerId == auth.currentUser?.uid
    }
}

package com.ahmetkaragunlu.financeai.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val session: AccountSession,
    authRepository: AuthRepository,
) : ViewModel() {
    val account = session.account

    val userName =
        account
            .flatMapLatest { current ->
                flow {
                    emit("")
                    if (current != null) {
                        val name = authRepository.getUserName()
                        if (session.isCurrent(current)) {
                            emit(name?.lowercase()?.replaceFirstChar { it.uppercase() }.orEmpty())
                        }
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0),
                "",
            )
}

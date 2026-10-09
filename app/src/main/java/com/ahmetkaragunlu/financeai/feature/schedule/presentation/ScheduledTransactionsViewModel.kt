package com.ahmetkaragunlu.financeai.feature.schedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.error.dataErrorMessageRes
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduledTransactionsViewModel
@Inject
constructor(
    private val session: AccountSession,
    private val complete: CompleteScheduledTransaction,
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
) : ViewModel() {
    private val mutableErrorResId = MutableStateFlow<Int?>(null)
    val errorResId = mutableErrorResId.asStateFlow()

    fun consumeError() { mutableErrorResId.value = null }

    val timeZoneId =
        session.account
            .map { it?.timeZoneId ?: ZoneId.systemDefault().id }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                session.account.value?.timeZoneId ?: ZoneId.systemDefault().id,
            )
    val scheduledTransactions: StateFlow<List<ScheduledTransaction>> =
        scheduledTransactionRepository
            .observeScheduledTransactions()
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun executeScheduledTransaction(scheduledTx: ScheduledTransaction) {
        val account = session.account.value?.takeIf { it.ownerId == scheduledTx.ownerId } ?: return
        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                complete(scheduledTx)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.isCurrent(account))
                    mutableErrorResId.value = dataErrorMessageRes(e) ?: R.string.error_operation_retry
            }
        }
    }
}

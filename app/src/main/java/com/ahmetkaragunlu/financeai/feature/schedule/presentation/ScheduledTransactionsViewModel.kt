package com.ahmetkaragunlu.financeai.feature.schedule.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.feature.schedule.domain.model.ScheduledTransaction
import com.ahmetkaragunlu.financeai.feature.schedule.domain.repository.ScheduledTransactionRepository
import com.ahmetkaragunlu.financeai.feature.schedule.domain.usecase.CompleteScheduledTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduledTransactionsViewModel @Inject constructor(
    private val session: AccountSession,
    private val complete: CompleteScheduledTransaction,
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
) : ViewModel() {
    val scheduledTransactions: StateFlow<List<ScheduledTransaction>> =
        scheduledTransactionRepository.observeScheduledTransactions()
            .distinctUntilChanged().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun executeScheduledTransaction(scheduledTx: ScheduledTransaction) {
        val account = session.account.value?.takeIf { it.ownerId == scheduledTx.ownerId } ?: return
        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                complete(scheduledTx)

            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("ScheduledTransactionsViewModel", "Scheduled completion failed (${e.javaClass.simpleName})") }
        }
    }

}

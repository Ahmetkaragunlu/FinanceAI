package com.ahmetkaragunlu.financeai.app.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.core.database.FinanceDatabase
import com.ahmetkaragunlu.financeai.core.sync.AccountSyncEngine
import com.ahmetkaragunlu.financeai.core.sync.SyncRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SyncConflictViewModel @Inject constructor(database: FinanceDatabase, private val engine: AccountSyncEngine) : ViewModel() {
    val conflicts = database.syncRecordDao().observeConflicts().stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), emptyList())
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()

    fun resolve(record: SyncRecord, keepLocal: Boolean) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableError.value = false
        viewModelScope.launch {
            try { engine.resolve(record, keepLocal) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableError.value = true }
            finally { mutableBusy.value = false }
        }
    }
}

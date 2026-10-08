package com.ahmetkaragunlu.financeai.app.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.app.navigation.deeplink.PendingDeepLink
import com.ahmetkaragunlu.financeai.app.navigation.deeplink.parseFinanceDeepLink
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.schedule.data.reminder.ReminderCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val calendar: FinanceCalendar,
    private val reminders: ReminderCoordinator,
) : ViewModel() {
    private val foregroundMutex = Mutex()

    // A single saveable record contains both the delivery id and URI; neither can be restored
    // alone.
    private val pendingRecord =
        savedStateHandle.getStateFlow<ArrayList<String>?>(PENDING_LINK, null)
    val pendingDeepLink: StateFlow<PendingDeepLink?> =
        pendingRecord
            .map(::decode)
            .stateIn(viewModelScope, SharingStarted.Eagerly, decode(pendingRecord.value))

    fun acceptInitialLink(uri: String?, previouslyConsumed: Boolean = false) {
        if (savedStateHandle.get<Boolean>(INITIAL_INTENT_HANDLED) == true) return
        savedStateHandle[INITIAL_INTENT_HANDLED] = true
        // Also respect the consumption flag saved by the earlier Activity-owned implementation.
        if (!previouslyConsumed) acceptNewLink(uri)
    }

    fun acceptNewLink(uri: String?) {
        if (uri == null || parseFinanceDeepLink(uri) == null) return
        savedStateHandle[PENDING_LINK] = arrayListOf(UUID.randomUUID().toString(), uri)
    }

    fun consumeDeepLink(id: String): Boolean {
        val record = savedStateHandle.get<ArrayList<String>>(PENDING_LINK) ?: return false
        if (record.firstOrNull() != id) return false
        savedStateHandle[PENDING_LINK] = null
        return true
    }

    /** Runs in the caller's Activity lifecycle, preserving cancellation on Activity destruction. */
    suspend fun onForeground() {
        calendar.refresh()
        // Every resume still refreshes; concurrent local scans cannot overlap.
        foregroundMutex.withLock { reminders.restoreCurrent() }
    }

    private fun decode(record: ArrayList<String>?): PendingDeepLink? {
        if (record == null || record.size != 2) return null
        val destination = parseFinanceDeepLink(record[1]) ?: return null
        return PendingDeepLink(record[0], destination)
    }

    private companion object {
        const val INITIAL_INTENT_HANDLED = "initial_intent_handled"
        const val PENDING_LINK = "pending_deep_link"
    }
}

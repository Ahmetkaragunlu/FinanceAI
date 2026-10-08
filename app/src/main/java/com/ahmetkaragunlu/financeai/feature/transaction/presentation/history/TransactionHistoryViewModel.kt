package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionHistoryViewModel
@Inject
constructor(private val repository: TransactionRepository, private val calendar: FinanceCalendar) :
    ViewModel() {
    private val mutableFilters = MutableStateFlow(HistoryFilters())
    val filters = mutableFilters.asStateFlow()
    var showCategoryError by mutableStateOf(false)
        private set

    val transactions: StateFlow<List<Transaction>> =
        filters
            .flatMapLatest { filter ->
                calendar
                    .observeFilterRange { filter.dateResId }
                    .flatMapLatest { range ->
                        val allDates = filter.dateResId == R.string.date
                        val start = if (allDates) 0L else range.first
                        val end = if (allDates) Long.MAX_VALUE else range.second
                        when {
                            filter.category != null ->
                                repository.observeTransactionsByCategoryAndDate(
                                    filter.category,
                                    start,
                                    end,
                                )
                            filter.type != null ->
                                repository.observeTransactionsByTypeAndDate(filter.type, start, end)
                            allDates -> repository.observeTransactions()
                            else -> repository.observeTransactionsByDateRange(start, end)
                        }
                    }
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onDateSelected(dateResId: Int) {
        mutableFilters.update { it.copy(dateResId = dateResId) }
    }

    fun onTypeSelected(type: TransactionType) {
        mutableFilters.update {
            it.copy(type = type, category = it.category.takeIf { _ -> it.type == type })
        }
        showCategoryError = false
    }

    fun onCategorySelected(category: CategoryType?) {
        mutableFilters.update { it.copy(category = category) }
        showCategoryError = false
    }

    fun canOpenCategoryMenu(): Boolean {
        val canOpen = filters.value.type != null
        showCategoryError = !canOpen
        return canOpen
    }
}

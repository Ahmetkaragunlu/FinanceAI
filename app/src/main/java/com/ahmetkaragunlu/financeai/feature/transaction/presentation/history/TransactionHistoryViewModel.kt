package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val calendar: FinanceCalendar
) : ViewModel() {
    var isHistoryPage by mutableStateOf(true)

    // Filter states
    var selectedDateResId by mutableIntStateOf(R.string.date)
        private set

    var selectedType by mutableStateOf<TransactionType?>(null)
        private set

    var selectedCategory by mutableStateOf<CategoryType?>(null)
        private set

    var isDateMenuOpen by mutableStateOf(false)
    var isTypeMenuOpen by mutableStateOf(false)
    var isCategoryMenuOpen by mutableStateOf(false)
    var showCategoryError by mutableStateOf(false)

    // Static options
    val dateOptions = listOf(
        R.string.today,
        R.string.yesterday,
        R.string.last_week,
        R.string.last_month,
        R.string.date
    )

    // Dynamic category options
    val categoryOptions: List<CategoryType>
        get() = selectedType?.let { type ->
            CategoryType.entries.filter { it.type == type }
        } ?: emptyList()

    // Internal trigger for flow refresh
    private val _filterTrigger = MutableStateFlow(0)

    // Transactions flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> =
        combine(_filterTrigger, calendar.observeFilterRange { selectedDateResId }) { _, range -> range }.flatMapLatest { range ->
            if (selectedDateResId == R.string.date) {
                when {
                    selectedCategory != null -> repository.observeTransactionsByCategoryAndDate(
                        selectedCategory!!, 0L, Long.MAX_VALUE
                    )
                    selectedType != null -> repository.observeTransactionsByTypeAndDate(
                        selectedType!!, 0L, Long.MAX_VALUE
                    )
                    else -> repository.observeTransactions()
                }
            }
            else {
                val (startDate, endDate) = range
                when {
                    selectedCategory != null -> repository.observeTransactionsByCategoryAndDate(
                        selectedCategory!!, startDate, endDate
                    )
                    selectedType != null -> repository.observeTransactionsByTypeAndDate(
                        selectedType!!, startDate, endDate
                    )
                    else -> repository.observeTransactionsByDateRange(startDate, endDate)
                }
            }
        }.distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter update functions
    fun onDateSelected(dateResId: Int) {
        selectedDateResId = dateResId
        isDateMenuOpen = false
        triggerRefresh()
    }

    fun onTypeSelected(type: TransactionType) {
        if (selectedType != type) {
            selectedCategory = null
        }
        selectedType = type
        isTypeMenuOpen = false
        showCategoryError = false
        triggerRefresh()
    }

    fun onCategorySelected(category: CategoryType?) {
        selectedCategory = category
        isCategoryMenuOpen = false
        showCategoryError = false
        triggerRefresh()
    }

    fun onCategoryDropdownClicked() {
        if (selectedType == null) {
            showCategoryError = true
            isCategoryMenuOpen = false
        } else {
            showCategoryError = false
            isCategoryMenuOpen = true
        }
    }

    private fun triggerRefresh() {
        _filterTrigger.value++
    }

    // Helper: Type label resource ID
    fun getTypeResId(type: TransactionType?): Int = when (type) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
        null -> R.string.type
    }
}

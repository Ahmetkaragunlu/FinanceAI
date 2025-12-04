package com.ahmetkaragunlu.financeai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: FinanceRepository
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
    val transactions: StateFlow<List<TransactionEntity>> =
        _filterTrigger.flatMapLatest {
            if (selectedDateResId == R.string.date) {
                when {
                    selectedCategory != null -> repository.getTransactionsByCategoryAndDate(
                        selectedCategory!!, 0L, System.currentTimeMillis()
                    )
                    selectedType != null -> repository.getTransactionsByTypeAndDate(
                        selectedType!!, 0L, System.currentTimeMillis()
                    )
                    else -> repository.getAllTransactions()
                }
            }
            else {
                val (startDate, endDate) = DateFormatter.getDateRange(selectedDateResId)
                when {
                    selectedCategory != null -> repository.getTransactionsByCategoryAndDate(
                        selectedCategory!!, startDate, endDate
                    )
                    selectedType != null -> repository.getTransactionsByTypeAndDate(
                        selectedType!!, startDate, endDate
                    )
                    else -> repository.getAllTransactionsByDateRange(startDate, endDate)
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
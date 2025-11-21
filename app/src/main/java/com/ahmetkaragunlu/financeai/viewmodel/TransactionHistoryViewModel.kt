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
import com.ahmetkaragunlu.financeai.utils.toResId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    var isHistoryPage by mutableStateOf(true)

    private val _selectedDateResId = MutableStateFlow(R.string.date)
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedCategory = MutableStateFlow<CategoryType?>(null)

    var isDateMenuOpen by mutableStateOf(false)
    var selectedDateResIdUI by mutableIntStateOf(R.string.date)

    var isTypeMenuOpen by mutableStateOf(false)
    var selectedTypeUI by mutableStateOf<TransactionType?>(null)

    var isCategoryMenuOpen by mutableStateOf(false)
    var selectedCategoryUI by mutableStateOf<CategoryType?>(null)
    var selectedCategoryResIdUI by mutableIntStateOf(R.string.category)
    var showCategoryError by mutableStateOf(false)

    val dateOptions = listOf(R.string.today, R.string.yesterday, R.string.last_week, R.string.last_month, R.string.date)
    val typeOptions = TransactionType.entries
    val categoryOptions: List<CategoryType>
        get() = selectedTypeUI?.let { type -> CategoryType.entries.filter { it.type == type } } ?: emptyList()


    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = combine(
        _selectedDateResId,
        _selectedType,
        _selectedCategory
    ) { dateResId, type, category ->
        Triple(dateResId, type, category)
    }.flatMapLatest { (dateResId, type, category) ->
        val (startDate, endDate) = getDateRange(dateResId)
        if (category != null) {
            repository.getTransactionsByCategoryAndDate(category, startDate, endDate)
        } else if (type != null) {
            repository.getTransactionsByTypeAndDate(type, startDate, endDate)
        } else {
            repository.getAllTransactionsByDateRange(startDate, endDate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun onDateOptionSelected(resId: Int) {
        isDateMenuOpen = false
        selectedDateResIdUI = resId
        _selectedDateResId.value = resId
    }

    fun onTypeSelected(type: TransactionType) {
        isTypeMenuOpen = false
        if (selectedTypeUI != type) {
            onCategorySelected(null)
        }
        selectedTypeUI = type
        _selectedType.value = type
        showCategoryError = false
    }

    fun onCategorySelected(category: CategoryType?) {
        isCategoryMenuOpen = false
        selectedCategoryUI = category
        selectedCategoryResIdUI = category?.toResId() ?: R.string.category
        _selectedCategory.value = category
    }

    fun onCategoryDropdownClicked() {
        if (selectedTypeUI == null) {
            showCategoryError = true
            isCategoryMenuOpen = false
        } else {
            showCategoryError = false
            isCategoryMenuOpen = true
        }
    }

    fun getTypeLabel(type: TransactionType?): Int {
        return when (type) {
            TransactionType.INCOME -> R.string.income
            TransactionType.EXPENSE -> R.string.expense
            null -> R.string.type
        }
    }

    private fun getDateRange(resId: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = System.currentTimeMillis()

        val start = when (resId) {
            R.string.today -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            R.string.yesterday -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            R.string.last_week -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            R.string.last_month -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            else -> 0L
        }
        if (resId == R.string.yesterday) {
            val yesterdayEnd = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }.timeInMillis
            return Pair(start, yesterdayEnd)
        }
        return Pair(start, end)
    }
}
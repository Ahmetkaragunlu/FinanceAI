package com.ahmetkaragunlu.financeai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.utils.toResId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
) : ViewModel() {

    var isHistoryPage by mutableStateOf(true)
    var isDateMenuOpen by mutableStateOf(false)
    var selectedDateResId by mutableIntStateOf(R.string.date)
    val dateOptions = listOf(
        R.string.today,
        R.string.yesterday,
        R.string.last_week,
        R.string.last_month,
        R.string.select_date
    )
    fun onDateOptionSelected(resId: Int) {
        isDateMenuOpen = false
        selectedDateResId = resId
    }
    var isTypeMenuOpen by mutableStateOf(false)
    var selectedType by mutableStateOf<TransactionType?>(null)
    val typeOptions = TransactionType.entries

    fun onTypeSelected(type: TransactionType) {
        isTypeMenuOpen = false
        if (selectedType != type) {
            selectedCategory = null
            selectedCategoryResId = R.string.category
        }
        selectedType = type
        showCategoryError = false
    }

    fun getTypeLabel(type: TransactionType?): Int {
        return when (type) {
            TransactionType.INCOME -> R.string.income
            TransactionType.EXPENSE -> R.string.expense
            null -> R.string.type
        }
    }

    var isCategoryMenuOpen by mutableStateOf(false)
    var selectedCategoryResId by mutableIntStateOf(R.string.category)
    var selectedCategory by mutableStateOf<CategoryType?>(null)
    var showCategoryError by mutableStateOf(false)

    val categoryOptions: List<CategoryType>
        get() = selectedType?.let { type ->
            CategoryType.entries.filter { it.type == type }
        } ?: emptyList()

    fun onCategoryDropdownClicked() {
        if (selectedType == null) {
            showCategoryError = true
            isCategoryMenuOpen = false
        } else {
            showCategoryError = false
            isCategoryMenuOpen = true
        }
    }
    fun onCategorySelected(category: CategoryType) {
        isCategoryMenuOpen = false
        selectedCategory = category
        selectedCategoryResId = category.toResId()
    }
}
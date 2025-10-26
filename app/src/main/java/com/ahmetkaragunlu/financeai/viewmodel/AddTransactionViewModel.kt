package com.ahmetkaragunlu.financeai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repo : FinanceRepository
) : ViewModel() {



    var selectedTransactionType by mutableStateOf(TransactionType.EXPENSE)

    var selectedCategory by mutableStateOf<CategoryType?>(null)

    var isCategoryDropdownExpanded by mutableStateOf(false)

    val availableCategories: List<CategoryType>
        get() = CategoryType.entries.filter { it.type == selectedTransactionType }


    var inputAmount by mutableStateOf("")
        private set

    var inputNote by mutableStateOf("")
        private set



    fun updateInputNote(note : String) {
        inputNote = note
    }
    fun updateInputAmount(amount: String) {
        inputAmount = amount
    }

    fun updateTransactionType(type: TransactionType) {
        selectedTransactionType = type
        selectedCategory = null
        inputNote = ""
        inputAmount = ""
    }

    fun updateCategory(category: CategoryType) {
        selectedCategory = category
    }

    fun toggleDropdown() {
        isCategoryDropdownExpanded = !isCategoryDropdownExpanded
    }

    fun dismissDropdown() {
        isCategoryDropdownExpanded = false
    }
}

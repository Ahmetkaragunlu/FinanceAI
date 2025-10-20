package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): StateFlow<List<TransactionEntity>> =
        repository.getAllTransactionsByDateRange(startDate, endDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun getTransactionsByCategoryAndDate(
        category: CategoryType,
        startDate: Long,
        endDate: Long
    ): StateFlow<List<TransactionEntity>> =
        repository.getTransactionsByCategoryAndDate(category, startDate, endDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun getTotalIncome(startDate: Long, endDate: Long): StateFlow<Double> =
        repository.getTotalIncomeByDateRange(startDate, endDate)
            .map { it ?: 0.0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0.0
            )

    fun getTotalExpense(startDate: Long, endDate: Long): StateFlow<Double> =
        repository.getTotalExpenseByDateRange(startDate, endDate)
            .map { it ?: 0.0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0.0
            )

    fun getCategoryExpenses(
        transactionType: String,
        startDate: Long,
        endDate: Long
    ): StateFlow<List<CategoryExpense>> =
        repository.getCategoryByTypeAndDateRange(transactionType, startDate, endDate)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun insertTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.insertTransaction(transaction) }

    fun deleteTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    fun updateTransaction(transaction: TransactionEntity) = viewModelScope.launch {
        repository.updateTransaction(transaction)
    }
}
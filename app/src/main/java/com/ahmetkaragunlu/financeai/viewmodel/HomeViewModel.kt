


package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.firebaserepo.FirebaseSyncService
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {




    // LAST MONTH FINANCIAL DATA

    private val lastMonthDateRange: Pair<Long, Long> = run {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.timeInMillis

        startDate to endDate
    }

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    private fun Flow<Double?>.toFormattedCurrency(): StateFlow<String> =
        this.map { currencyFormat.format(it ?: 0.0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = currencyFormat.format(0.0)
            )


    val lastMonthIncomeFormatted: StateFlow<String> =
        repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
            .toFormattedCurrency()

    val lastMonthExpenseFormatted: StateFlow<String> =
        repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
            .toFormattedCurrency()

    val lastMonthRemainingBalance: StateFlow<Double> =
        combine(
            repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
            repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
        ) { income, expense ->
            (income ?: 0.0) - (expense ?: 0.0)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val lastMonthRemainingBalanceFormatted: StateFlow<String> =
        lastMonthRemainingBalance.toFormattedCurrency()



    val spendingPercentage: StateFlow<Double> =
        combine(
            repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
            repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
        ) { income, expense ->
            val incomeValue = income ?: 0.0
            val expenseValue = expense ?: 0.0
            if (incomeValue > 0) {
                (incomeValue - expenseValue) / incomeValue
            } else {
                0.0
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )


    val homeUiState: StateFlow<HomeUiState> = combine(
        lastMonthIncomeFormatted,
        lastMonthExpenseFormatted,
        lastMonthRemainingBalance,
        lastMonthRemainingBalanceFormatted,
        spendingPercentage
    ) { income, expense, balance, balanceFormatted, percentage ->
        HomeUiState(
            totalIncome = income,
            totalExpense = expense,
            remainingBalance = balance,
            remainingBalanceFormatted = balanceFormatted,
            spendingPercentage = percentage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    val lastMonthCategoryExpenses: StateFlow<List<CategoryExpense>> =
        repository.getCategoryByTypeAndDateRange(
            transactionType = TransactionType.EXPENSE.name,
            startDate = lastMonthDateRange.first,
            endDate = lastMonthDateRange.second
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

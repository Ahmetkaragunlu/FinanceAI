package com.ahmetkaragunlu.financeai.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.core.format.formatAsCurrency
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.FinancialSummary
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    calendar: FinanceCalendar,
    private val session: AccountSession,
    repository: TransactionRepository,
    budgetRepository: BudgetRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val month = calendar.observeMonth()
    private val summary: StateFlow<FinancialSummary?> =
        month
            .flatMapLatest { repository.observeFinancialSummary(it.start, it.endExclusive) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val userName: StateFlow<String> =
        session.account
            .flatMapLatest { account ->
                flow {
                    if (account == null) emit("")
                    else {
                        val name = authRepository.getUserName()
                        emit(
                            if (session.isCurrent(account))
                                name?.lowercase()?.replaceFirstChar { it.uppercase() }.orEmpty()
                            else ""
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val homeUiState: StateFlow<HomeUiState> =
        combine(summary, session.account) { value, account ->
                if (value == null || account == null) return@combine HomeUiState()
                val currency = account?.currencyCode ?: "XXX"
                HomeUiState(
                    totalIncome = value.income.formatAsCurrency(currency),
                    totalExpense = value.expense.formatAsCurrency(currency),
                    remainingBalance = value.remainingBalance,
                    remainingBalanceFormatted = value.remainingBalance.formatAsCurrency(currency),
                    remainingIncomeRatio = value.remainingIncomeRatio,
                )
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val monthlyCategoryExpenses: StateFlow<List<CategoryExpense>?> =
        month
            .flatMapLatest {
                repository.observeCategoryExpensesByTypeAndDateRange(
                    TransactionType.EXPENSE,
                    it.start,
                    it.endExclusive,
                )
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val aiSuggestion: StateFlow<AiSuggestionState> =
        combine(budgetRepository.observeBudgets(), summary, monthlyCategoryExpenses) {
                budgets,
                value,
                expenses ->
                if (value == null || expenses == null) AiSuggestionState.Analyze
                else buildAiSuggestion(budgets, value.expense, expenses)
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AiSuggestionState.Analyze,
            )
}

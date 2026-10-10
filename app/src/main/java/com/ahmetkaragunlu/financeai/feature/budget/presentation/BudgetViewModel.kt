package com.ahmetkaragunlu.financeai.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.money.MoneyAmounts
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper.budgetErrorMessageRes
import com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper.mapBudgetUiState
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val calendar: FinanceCalendar,
    private val session: AccountSession,
    private val budgetRepository: BudgetRepository,
    private val financeRepository: TransactionRepository
) : ViewModel() {

    private var isSaving = false
    private val mutableErrorResId = MutableStateFlow<Int?>(null)
    val errorResId = mutableErrorResId.asStateFlow()

    fun consumeError() {
        mutableErrorResId.value = null
    }
    private val month = calendar.observeMonth()
    private val _formState = MutableStateFlow(BudgetFormState())
    val formState = _formState.asStateFlow()
    private val _deleteDialogState = MutableStateFlow(DeleteDialogState())
    val deleteDialogState = _deleteDialogState.asStateFlow()
    private val budgetRulesFlow = budgetRepository.observeBudgets()
    private val summaryFlow = month.flatMapLatest {
        financeRepository.observeFinancialSummary(it.start, it.endExclusive)
    }
    private val categoryExpensesFlow = month.flatMapLatest {
        financeRepository.observeCategoryExpensesByTypeAndDateRange(
            TransactionType.EXPENSE,
            it.start,
            it.endExclusive
        )
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRulesFlow,
        summaryFlow,
        categoryExpensesFlow
    ) { rules, summary, categoryExpenses ->
        if (rules.isEmpty()) {
            BudgetUiState(isBudgetEmpty = true)
        } else {
            mapBudgetUiState(rules, summary.income, summary.expense, categoryExpenses)
        }
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun onEvent(event: BudgetEvent) {
        when (event) {
            is BudgetEvent.OnAmountChange -> _formState.update {
                it.copy(
                    amountInput = event.amount,
                    amountErrorResId = null
                )
            }

            is BudgetEvent.OnPercentageChange -> _formState.update {
                it.copy(
                    percentageInput = event.percentage,
                    amountErrorResId = null
                )
            }

            is BudgetEvent.OnTypeChange -> _formState.update { it.copy(selectedType = event.type) }
            is BudgetEvent.OnCategoryChange -> _formState.update {
                it.copy(
                    selectedCategory = event.category,
                    categoryErrorResId = null
                )
            }

            BudgetEvent.OnAddBudgetClick -> resetAndOpenForm(isGeneral = false)
            BudgetEvent.OnCreateGeneralBudgetClick -> resetAndOpenForm(isGeneral = true)
            is BudgetEvent.OnEditGeneralClick -> openFormForEditing(
                id = event.state.id,
                type = BudgetType.GENERAL_MONTHLY,
                amount = event.state.limitAmount,
                category = null
            )

            is BudgetEvent.OnEditCategoryClick -> openFormForEditing(
                id = event.state.id,
                type = event.state.budgetType,
                amount = event.state.limitAmount,
                percentage = event.state.limitPercentage,
                category = event.state.category
            )

            is BudgetEvent.OnSaveClick -> validateAndSave()
            BudgetEvent.OnDismissBottomSheet -> _formState.update { it.copy(isVisible = false) }
            is BudgetEvent.OnDeleteClick -> _deleteDialogState.update {
                it.copy(isVisible = true, budgetIdToDelete = event.id)
            }

            BudgetEvent.OnConfirmDelete -> deleteBudgetRule()
            BudgetEvent.OnDismissDeleteDialog -> _deleteDialogState.update {
                it.copy(isVisible = false, budgetIdToDelete = null)
            }

            BudgetEvent.OnDismissConflictDialog -> _formState.update {
                it.copy(isConflictDialogOpen = false)
            }
        }
    }

    private fun validateAndSave() {
        val currentState = _formState.value
        var hasError = false

        if (
            currentState.selectedType != BudgetType.GENERAL_MONTHLY &&
            currentState.selectedCategory == null
        ) {
            _formState.update { it.copy(categoryErrorResId = R.string.error_select_category) }
            hasError = true
        }

        if (currentState.selectedType == BudgetType.CATEGORY_PERCENTAGE) {
            if (
                currentState.percentageInput.replace(',', '.').toDoubleOrNull()
                    ?.let { it.isFinite() && it > 0 } != true
            ) {
                _formState.update { it.copy(amountErrorResId = R.string.error_enter_percent) }
                hasError = true
            }
        } else {
            if (MoneyAmounts.parse(currentState.amountInput, session.requireAccount().currencyCode) == null) {
                _formState.update { it.copy(amountErrorResId = R.string.error_invalid_amount) }
                hasError = true
            }
        }
        if (!hasError) {
            saveBudgetRule()
        }
    }

    private fun saveBudgetRule() {
        if (isSaving) return
        val account = session.account.value ?: return
        val currentState = _formState.value
        isSaving = true
        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                val amount =
                    MoneyAmounts.parse(currentState.amountInput, account.currencyCode) ?: 0.0
                val percentage = currentState.percentageInput.replace(',', '.').toDoubleOrNull()
                val hasConflict = checkConflict(currentState)
                if (!session.isCurrent(account)) return@launch
                if (hasConflict) {
                    val errorRes = if (currentState.selectedType == BudgetType.GENERAL_MONTHLY)
                        R.string.error_conflict_general
                    else
                        R.string.error_conflict_category
                    _formState.update {
                        it.copy(
                            isConflictDialogOpen = true,
                            conflictErrorResId = errorRes
                        )
                    }
                } else {
                    var firestoreId = ""
                    if (currentState.editingId != 0) {
                        val existingRules =
                            budgetRepository.observeBudgets().firstOrNull() ?: emptyList()
                        val existingRule = existingRules.find { it.id == currentState.editingId }
                        firestoreId = existingRule?.firestoreId ?: ""
                    }
                    if (!session.isCurrent(account)) return@launch
                    val entity = Budget(
                        ownerId = account.ownerId,
                        currencyCode = account.currencyCode,
                        id = currentState.editingId,
                        budgetType = currentState.selectedType,
                        amount = amount,
                        category = currentState.selectedCategory,
                        limitPercentage = percentage,
                        firestoreId = firestoreId,
                        syncedToFirebase = false
                    )
                    budgetRepository.insertBudget(entity)

                    if (session.isCurrent(account)) {
                        _formState.update {
                            it.copy(isVisible = false, isConflictDialogOpen = false)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.isCurrent(account)) _formState.update {
                    if (e is BudgetException.DuplicateRule) it.copy(
                        isConflictDialogOpen = true,
                        conflictErrorResId = if (currentState.selectedType == BudgetType.GENERAL_MONTHLY)
                            R.string.error_conflict_general else R.string.error_conflict_category
                    ) else it.copy(amountErrorResId = budgetErrorMessageRes(e))
                }
            } finally {
                isSaving = false
            }
        }
    }

    private fun resetAndOpenForm(isGeneral: Boolean) {
        _formState.update {
            BudgetFormState(
                isVisible = true,
                selectedType =
                    if (isGeneral) BudgetType.GENERAL_MONTHLY else BudgetType.CATEGORY_AMOUNT,
                editingId = 0,
                amountErrorResId = null,
                categoryErrorResId = null
            )
        }
    }

    private fun openFormForEditing(
        id: Int,
        type: BudgetType,
        amount: Double,
        percentage: Double? = null,
        category: CategoryType?
    ) {
        _formState.update {
            BudgetFormState(
                isVisible = true,
                editingId = id,
                selectedType = type,
                amountInput = BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString(),
                percentageInput = percentage?.let {
                    BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()
                } ?: "",
                selectedCategory = category,
                amountErrorResId = null,
                categoryErrorResId = null
            )
        }
    }

    private suspend fun checkConflict(state: BudgetFormState): Boolean {
        if (state.selectedType == BudgetType.GENERAL_MONTHLY) {
            val existing = budgetRepository.observeGeneralBudget().firstOrNull()
            return existing != null && existing.id != state.editingId
        } else if (state.selectedCategory != null) {
            val existing = budgetRepository.getBudgetByCategory(state.selectedCategory)
            return existing != null && existing.id != state.editingId
        }
        return false
    }

    private fun deleteBudgetRule() {
        val account = session.account.value ?: return
        val id = _deleteDialogState.value.budgetIdToDelete ?: return
        viewModelScope.launch {
            try {
                if (!session.isCurrent(account)) return@launch
                run {
                    val rules = budgetRepository.observeBudgets().firstOrNull() ?: emptyList()
                    val budgetToDelete = rules.find { it.id == id }

                    budgetToDelete?.let { budget ->
                        budgetRepository.deleteBudget(budget)

                    }
                }
                if (session.isCurrent(account)) {
                    _deleteDialogState.update {
                        it.copy(isVisible = false, budgetIdToDelete = null)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.isCurrent(account)) mutableErrorResId.value = budgetErrorMessageRes(e)
            }
        }
    }



}

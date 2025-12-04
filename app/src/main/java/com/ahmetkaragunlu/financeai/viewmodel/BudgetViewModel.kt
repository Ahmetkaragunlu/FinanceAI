package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.firebasesync.FirebaseSyncService
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.screens.main.budget.BudgetEvent
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val financeRepository: FinanceRepository,
    private val firebaseSyncService: FirebaseSyncService
) : ViewModel() {
    private val currentMonthRange = DateFormatter.getCurrentMonthRange()
    private val _formState = MutableStateFlow(BudgetFormState())
    val formState = _formState.asStateFlow()
    private val _deleteDialogState = MutableStateFlow(DeleteDialogState())
    val deleteDialogState = _deleteDialogState.asStateFlow()
    private val budgetRulesFlow = budgetRepository.getAllBudgets()
    private val totalIncomeFlow = financeRepository.getTotalIncomeByDateRange(
        currentMonthRange.first, currentMonthRange.second
    )
    private val totalExpenseFlow = financeRepository.getTotalExpenseByDateRange(
        currentMonthRange.first, currentMonthRange.second
    )
    private val categoryExpensesFlow = financeRepository.getCategoryByTypeAndDateRange(
        TransactionType.EXPENSE, currentMonthRange.first, currentMonthRange.second
    )

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRulesFlow,
        totalIncomeFlow,
        totalExpenseFlow,
        categoryExpensesFlow
    ) { rules, totalIncome, totalExpense, categoryExpenses ->
        if (rules.isEmpty()) {
            BudgetUiState(isBudgetEmpty = true)
        } else {
            calculateBudgetState(rules, totalIncome ?: 0.0, totalExpense ?: 0.0, categoryExpenses)
        }
    }.debounce(300).distinctUntilChanged().stateIn(
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

        if (currentState.selectedType != BudgetType.GENERAL_MONTHLY && currentState.selectedCategory == null) {
            _formState.update { it.copy(categoryErrorResId = R.string.error_select_category) }
            hasError = true
        }

        if (currentState.selectedType == BudgetType.CATEGORY_PERCENTAGE) {
            if (currentState.percentageInput.isBlank()) {
                _formState.update { it.copy(amountErrorResId = R.string.error_enter_percent) }
                hasError = true
            }
        } else {
            if (currentState.amountInput.isBlank() || currentState.amountInput.toDoubleOrNull() == 0.0) {
                _formState.update { it.copy(amountErrorResId = R.string.error_enter_valid_amount) }
                hasError = true
            }
        }
        if (!hasError) {
            saveBudgetRule()
        }
    }

    private fun saveBudgetRule() {
        viewModelScope.launch {
            val currentState = _formState.value
            val amount = currentState.amountInput.toDoubleOrNull() ?: 0.0
            val percentage = currentState.percentageInput.toDoubleOrNull()
            if (checkConflict(currentState)) {
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
                        budgetRepository.getAllBudgets().firstOrNull() ?: emptyList()
                    val existingRule = existingRules.find { it.id == currentState.editingId }
                    firestoreId = existingRule?.firestoreId ?: ""
                }
                if (firestoreId.isEmpty()) {
                    firestoreId = firebaseSyncService.getNewBudgetId()
                }
                val entity = BudgetEntity(
                    id = currentState.editingId,
                    budgetType = currentState.selectedType,
                    amount = amount,
                    category = currentState.selectedCategory,
                    limitPercentage = percentage,
                    firestoreId = firestoreId,
                    syncedToFirebase = false
                )
                budgetRepository.insertBudget(entity)
                launch {
                    firebaseSyncService.syncBudgetToFirebase(entity).onSuccess {
                        budgetRepository.updateBudget(entity.copy(syncedToFirebase = true))
                    }
                }

                _formState.update { it.copy(isVisible = false, isConflictDialogOpen = false) }
            }
        }
    }

    private fun resetAndOpenForm(isGeneral: Boolean) {
        _formState.update {
            BudgetFormState(
                isVisible = true,
                selectedType = if (isGeneral) BudgetType.GENERAL_MONTHLY else BudgetType.CATEGORY_AMOUNT,
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
                amountInput = amount.toInt().toString(),
                percentageInput = percentage?.toInt()?.toString() ?: "",
                selectedCategory = category,
                amountErrorResId = null,
                categoryErrorResId = null
            )
        }
    }

    private suspend fun checkConflict(state: BudgetFormState): Boolean {
        if (state.selectedType == BudgetType.GENERAL_MONTHLY) {
            val existing = budgetRepository.getGeneralBudget().firstOrNull()
            return existing != null && existing.id != state.editingId
        } else if (state.selectedCategory != null) {
            val existing = budgetRepository.getBudgetByCategory(state.selectedCategory)
            return existing != null && existing.id != state.editingId
        }
        return false
    }

    private fun deleteBudgetRule() {
        viewModelScope.launch {
            _deleteDialogState.value.budgetIdToDelete?.let { id ->
                val rules = budgetRepository.getAllBudgets().firstOrNull() ?: emptyList()
                val budgetToDelete = rules.find { it.id == id }

                budgetToDelete?.let { budget ->
                    val firestoreId = budget.firestoreId
                    budgetRepository.deleteBudget(budget)

                    if (firestoreId.isNotEmpty()) {
                        launch {
                            firebaseSyncService.deleteBudgetFromFirebase(firestoreId)
                        }
                    }
                }
            }
            _deleteDialogState.update { it.copy(isVisible = false, budgetIdToDelete = null) }
        }
    }

    private fun calculateBudgetState(
        rules: List<BudgetEntity>,
        totalIncome: Double,
        totalExpense: Double,
        categoryExpenses: List<com.ahmetkaragunlu.financeai.roommodel.CategoryExpense>
    ): BudgetUiState {
        val generalRule = rules.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
        val generalBudgetState = generalRule?.let { rule ->
            val limit = rule.amount
            val spent = totalExpense
            val progress = if (limit > 0) (spent / limit).toFloat() else 0f
            GeneralBudgetState(
                id = rule.id,
                limitAmount = limit,
                spentAmount = spent,
                remainingAmount = limit - spent,
                progress = progress,
                incomeAmount = totalIncome,
                expenseAmount = spent
            )
        }
        val categoryBudgetStates = rules.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }
            .map { rule ->
                val categoryName = rule.category?.name
                val spent =
                    categoryExpenses.find { it.category == categoryName }?.totalAmount ?: 0.0
                val limit =
                    if (rule.budgetType == BudgetType.CATEGORY_PERCENTAGE && generalRule != null) {
                        generalRule.amount * ((rule.limitPercentage ?: 0.0) / 100)
                    } else {
                        rule.amount
                    }
                val progress = if (limit > 0) (spent / limit).toFloat() else 0f
                CategoryBudgetState(
                    id = rule.id,
                    category = rule.category ?: CategoryType.OTHER,
                    budgetType = rule.budgetType,
                    limitAmount = limit,
                    limitPercentage = rule.limitPercentage,
                    spentAmount = spent,
                    progress = progress.coerceIn(0f, 1f),
                    isOverBudget = spent > limit,
                    percentageUsed = if (limit > 0) ((spent / limit) * 100).toInt() else 0
                )
            }.sortedByDescending { it.percentageUsed }
        val warning = generateWarning(categoryBudgetStates, generalBudgetState)
        return BudgetUiState(
            isBudgetEmpty = false,
            generalBudgetState = generalBudgetState,
            categoryBudgetStates = categoryBudgetStates,
            warningMessageResId = warning.first,
            warningMessageArgs = warning.second
        )
    }

    private fun generateWarning(
        categories: List<CategoryBudgetState>,
        general: GeneralBudgetState?
    ): Pair<Int?, List<Any>> {
        val generalOver = general != null && general.remainingAmount < 0
        val overBudgetCategories = categories.filter { it.isOverBudget }
        val overCount = overBudgetCategories.size

        return when {
            generalOver && overCount > 0 -> {
                if (overCount == 1) {
                    Pair(
                        R.string.warning_budget_and_category_exceeded,
                        listOf(overBudgetCategories.first().category)
                    )
                } else {
                    Pair(R.string.warning_budget_and_multiple_categories, listOf(overCount))
                }
            }

            generalOver -> Pair(R.string.warning_budget_exceeded, emptyList())
            overCount > 0 -> {
                if (overCount == 1) {
                    Pair(
                        R.string.warning_category_exceeded,
                        listOf(overBudgetCategories.first().category)
                    )
                } else {
                    Pair(R.string.warning_multiple_categories_exceeded, listOf(overCount))
                }
            }

            general != null && general.progress > 0.85f -> Pair(
                R.string.warning_budget_near_end,
                emptyList()
            )

            else -> Pair(null, emptyList())
        }
    }
}
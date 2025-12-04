package com.ahmetkaragunlu.financeai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val currentMonthRange = DateFormatter.getCurrentMonthRange()

    // Dialog States
    var showConflictDialog by mutableStateOf(false)
        private set
    var conflictMessage by mutableStateOf("")
        private set
    var showDeleteDialog by mutableStateOf(false)
        private set
    var budgetIdToDelete by mutableStateOf<Int?>(null)
        private set

    // Bottom Sheet States
    var showBottomSheet by mutableStateOf(false)
        private set
    var editingBudgetId by mutableStateOf(0)
        private set

    // Bottom Sheet Form States
    var selectedType by mutableStateOf(BudgetType.CATEGORY_AMOUNT)
        private set
    var inputAmount by mutableStateOf("")
        private set
    var inputPercentage by mutableStateOf("")
        private set
    var selectedCategory by mutableStateOf<CategoryType?>(null)
        private set

    private val budgetRulesFlow = budgetRepository.getAllBudgets()
    private val totalIncomeFlow = financeRepository.getTotalIncomeByDateRange(
        currentMonthRange.first,
        currentMonthRange.second
    )
    private val totalExpenseFlow = financeRepository.getTotalExpenseByDateRange(
        currentMonthRange.first,
        currentMonthRange.second
    )
    private val categoryExpensesFlow = financeRepository.getCategoryByTypeAndDateRange(
        TransactionType.EXPENSE,
        currentMonthRange.first,
        currentMonthRange.second
    )

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRulesFlow,
        totalIncomeFlow,
        totalExpenseFlow,
        categoryExpensesFlow
    ) { rules, totalIncome, totalExpense, categoryExpenses ->

        if (rules.isEmpty()) {
            return@combine BudgetUiState(isBudgetEmpty = true)
        }

        val generalRule = rules.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
        val generalBudgetState = generalRule?.let { rule ->
            val limit = rule.amount
            val spent = totalExpense ?: 0.0
            val remaining = limit - spent
            val progress = if (limit > 0) (spent / limit).toFloat() else 0f

            GeneralBudgetState(
                id = rule.id,
                limitAmount = limit,
                spentAmount = spent,
                remainingAmount = remaining,
                progress = progress,
                incomeAmount = totalIncome ?: 0.0,
                expenseAmount = spent
            )
        }

        val categoryBudgetStates = rules.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }
            .map { rule ->
                val categoryName = rule.category?.name
                val categorySpent =
                    categoryExpenses.find { it.category == categoryName }?.totalAmount ?: 0.0

                val limit =
                    if (rule.budgetType == BudgetType.CATEGORY_PERCENTAGE && generalRule != null) {
                        generalRule.amount * ((rule.limitPercentage ?: 0.0) / 100)
                    } else {
                        rule.amount
                    }

                val progress = if (limit > 0) (categorySpent / limit).toFloat() else 0f
                val isOverBudget = categorySpent > limit
                val percentageUsed = if (limit > 0) ((categorySpent / limit) * 100).toInt() else 0

                CategoryBudgetState(
                    id = rule.id,
                    category = rule.category ?: CategoryType.OTHER,
                    budgetType = rule.budgetType,
                    limitAmount = limit,
                    limitPercentage = rule.limitPercentage,
                    spentAmount = categorySpent,
                    progress = progress.coerceIn(0f, 1f),
                    isOverBudget = isOverBudget,
                    percentageUsed = percentageUsed
                )
            }.sortedByDescending { it.percentageUsed }

        val warningMessage = generateLocalWarning(categoryBudgetStates, generalBudgetState)

        BudgetUiState(
            isBudgetEmpty = false,
            generalBudgetState = generalBudgetState,
            categoryBudgetStates = categoryBudgetStates,
            aiWarningMessage = warningMessage
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState()
    )

    private fun generateLocalWarning(
        categories: List<CategoryBudgetState>,
        general: GeneralBudgetState?
    ): String? {
        val generalOver = general != null && general.remainingAmount < 0
        val overBudgetCategories = categories.filter { it.isOverBudget }
        val overCount = overBudgetCategories.size

        return when {
            generalOver && overCount > 0 -> {
                val catText =
                    if (overCount == 1) overBudgetCategories.first().category.name else "$overCount kategori"
                "Harcama limiti ve $catText aşıldı!"
            }

            generalOver -> "Harcama limitinizi aştınız!"
            overCount > 0 -> {
                if (overCount == 1) "${overBudgetCategories.first().category.name} limitini aştınız!" else "$overCount kategoride limit aşıldı!"
            }

            general != null && general.progress > 0.85f -> "Genel bütçenin sonuna yaklaştınız."
            else -> null
        }
    }

    fun updateSelectedType(type: BudgetType) {
        selectedType = type
    }

    fun updateInputAmount(amount: String) {
        inputAmount = amount
    }

    fun updateInputPercentage(percentage: String) {
        inputPercentage = percentage
    }

    fun updateSelectedCategory(category: CategoryType?) {
        selectedCategory = category
    }

    fun openAddBudgetSheet() {
        editingBudgetId = 0
        selectedType = BudgetType.CATEGORY_AMOUNT
        inputAmount = ""
        inputPercentage = ""
        selectedCategory = null
        showBottomSheet = true
    }

    fun openCreateGeneralBudgetSheet() {
        editingBudgetId = 0
        selectedType = BudgetType.GENERAL_MONTHLY
        inputAmount = ""
        inputPercentage = ""
        selectedCategory = null
        showBottomSheet = true
    }

    fun openEditGeneralBudgetSheet(state: GeneralBudgetState) {
        editingBudgetId = state.id
        selectedType = BudgetType.GENERAL_MONTHLY
        inputAmount = state.limitAmount.toInt().toString()
        inputPercentage = ""
        selectedCategory = null
        showBottomSheet = true
    }

    fun openEditCategoryBudgetSheet(state: CategoryBudgetState) {
        editingBudgetId = state.id
        selectedType = state.budgetType
        inputAmount = state.limitAmount.toInt().toString()
        inputPercentage = state.limitPercentage?.toInt()?.toString() ?: ""
        selectedCategory = state.category
        showBottomSheet = true
    }

    fun closeBottomSheet() {
        showBottomSheet = false
    }

    fun openDeleteDialog(id: Int) {
        budgetIdToDelete = id
        showDeleteDialog = true
    }

    fun closeDeleteDialog() {
        showDeleteDialog = false
        budgetIdToDelete = null
    }

    fun addBudgetRule() {
        viewModelScope.launch {
            val amount = inputAmount.toDoubleOrNull() ?: 0.0
            val percentage = inputPercentage.toDoubleOrNull()

            var hasConflict = false
            if (selectedType == BudgetType.GENERAL_MONTHLY) {
                val existing = budgetRepository.getGeneralBudget().firstOrNull()
                if (existing != null && existing.id != editingBudgetId) hasConflict = true
            } else if (selectedCategory != null) {
                val existing = budgetRepository.getBudgetByCategory(selectedCategory!!)
                if (existing != null && existing.id != editingBudgetId) hasConflict = true
            }

            if (hasConflict) {
                conflictMessage =
                    if (selectedType == BudgetType.GENERAL_MONTHLY) "Genel Bütçe" else selectedCategory?.name ?: "Bu kategori"
                showConflictDialog = true
            } else {
                val entity = BudgetEntity(
                    id = editingBudgetId,
                    budgetType = selectedType,
                    amount = amount,
                    category = selectedCategory,
                    limitPercentage = percentage,
                    firestoreId = "",
                    syncedToFirebase = false
                )
                budgetRepository.insertBudget(entity)
                showConflictDialog = false
                closeBottomSheet()
            }
        }
    }

    fun deleteBudgetRule() {
        viewModelScope.launch {
            budgetIdToDelete?.let { id ->
                val currentList = budgetRepository.getAllBudgets().firstOrNull() ?: emptyList()
                val itemToDelete = currentList.find { it.id == id }
                itemToDelete?.let { budgetRepository.deleteBudget(it) }
            }
            closeDeleteDialog()
        }
    }

    fun closeConflictDialog() {
        showConflictDialog = false
    }
}
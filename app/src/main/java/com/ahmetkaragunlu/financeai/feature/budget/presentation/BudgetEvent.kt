package com.ahmetkaragunlu.financeai.feature.budget.presentation

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType


sealed interface BudgetEvent {
    data class OnAmountChange(val amount: String) : BudgetEvent
    data class OnPercentageChange(val percentage: String) : BudgetEvent
    data class OnTypeChange(val type: BudgetType) : BudgetEvent
    data class OnCategoryChange(val category: CategoryType?) : BudgetEvent
    data object OnAddBudgetClick : BudgetEvent
    data object OnCreateGeneralBudgetClick : BudgetEvent
    data class OnEditGeneralClick(val state: GeneralBudgetState) : BudgetEvent
    data class OnEditCategoryClick(val state: CategoryBudgetState) : BudgetEvent
    data class OnDeleteClick(val id: Int) : BudgetEvent
    data object OnConfirmDelete : BudgetEvent
    data object OnSaveClick : BudgetEvent
    data object OnDismissBottomSheet : BudgetEvent
    data object OnDismissDeleteDialog : BudgetEvent
    data object OnDismissConflictDialog : BudgetEvent
}

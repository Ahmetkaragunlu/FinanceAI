package com.ahmetkaragunlu.financeai.screens.main.budget

import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.viewmodel.CategoryBudgetState
import com.ahmetkaragunlu.financeai.viewmodel.GeneralBudgetState

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
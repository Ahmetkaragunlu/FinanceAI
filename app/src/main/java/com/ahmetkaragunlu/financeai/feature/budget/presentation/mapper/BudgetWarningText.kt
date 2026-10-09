package com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetWarning
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toLabelResId

@Composable
internal fun BudgetWarning.localizedText(): String = when (this) {
    BudgetWarning.GeneralExceeded -> stringResource(R.string.warning_budget_exceeded)
    BudgetWarning.GeneralNearLimit -> stringResource(R.string.warning_budget_near_end)
    is BudgetWarning.CategoryExceeded -> stringResource(R.string.warning_category_exceeded, stringResource(category.toLabelResId()))
    is BudgetWarning.CategoriesExceeded -> stringResource(R.string.warning_multiple_categories_exceeded, count)
    is BudgetWarning.GeneralAndCategoryExceeded -> stringResource(R.string.warning_budget_and_category_exceeded, stringResource(category.toLabelResId()))
    is BudgetWarning.GeneralAndCategoriesExceeded -> stringResource(R.string.warning_budget_and_multiple_categories, count)
}

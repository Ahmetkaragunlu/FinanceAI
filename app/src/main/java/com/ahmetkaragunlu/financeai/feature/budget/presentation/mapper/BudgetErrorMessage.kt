package com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.error.dataErrorMessageRes
import com.ahmetkaragunlu.financeai.feature.budget.domain.error.BudgetException

@StringRes
fun budgetErrorMessageRes(error: Throwable): Int = when (error) {
    is BudgetException.DuplicateRule -> R.string.budget_rule_exists_error
    else -> dataErrorMessageRes(error) ?: R.string.failure
}

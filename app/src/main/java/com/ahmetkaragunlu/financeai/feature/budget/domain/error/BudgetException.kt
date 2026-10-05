package com.ahmetkaragunlu.financeai.feature.budget.domain.error

sealed class BudgetException : Exception() {
    class DuplicateRule : BudgetException()
}

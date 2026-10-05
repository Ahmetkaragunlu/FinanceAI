package com.ahmetkaragunlu.financeai.feature.home.presentation

data class HomeUiState(
    val totalIncome: String = "",
    val totalExpense: String = "",
    val remainingBalance: Double = 0.0,
    val remainingBalanceFormatted: String = "",
    val remainingIncomeRatio: Double = 0.0
)

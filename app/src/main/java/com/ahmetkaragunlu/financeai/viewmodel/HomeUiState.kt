package com.ahmetkaragunlu.financeai.viewmodel

data class HomeUiState(
    val totalIncome: String = "",
    val totalExpense: String = "",
    val remainingBalance: Double = 0.0,
    val remainingBalanceFormatted: String = "",
    val spendingPercentage: Double = 0.0
)
package com.ahmetkaragunlu.financeai.feature.transaction.navigation

import kotlinx.serialization.Serializable

@Serializable data object TransactionHistoryDestination

@Serializable data class TransactionDetailDestination(val transactionId: Int)

@Serializable data object AddTransactionDestination

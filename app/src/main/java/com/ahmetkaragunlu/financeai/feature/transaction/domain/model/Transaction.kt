package com.ahmetkaragunlu.financeai.feature.transaction.domain.model


data class Transaction(
    val id: Int = 0,
    val firestoreId: String = "",
    val amount: Double = 0.0,
    val transaction: TransactionType,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val category: CategoryType,
    val photoUri: String? = null,
    val locationFull: String? = null,
    val locationShort: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val syncedToFirebase: Boolean = false
)

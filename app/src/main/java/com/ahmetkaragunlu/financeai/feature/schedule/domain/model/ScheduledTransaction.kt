package com.ahmetkaragunlu.financeai.feature.schedule.domain.model

import com.ahmetkaragunlu.financeai.core.money.UNSPECIFIED_CURRENCY
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

data class ScheduledTransaction(
    val id: Long = 0,
    val firestoreId: String = "",
    val amount: Double,
    val type: TransactionType,
    val category: CategoryType,
    val note: String?,
    val scheduledDate: Long,
    val expirationNotificationSent: Boolean = false,
    val notificationSent: Boolean = false,
    val photoUri: String? = null,
    val locationFull: String? = null,
    val locationShort: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ownerId: String = "",
    val currencyCode: String = UNSPECIFIED_CURRENCY,
    val syncedToFirebase: Boolean = false
)

package com.ahmetkaragunlu.financeai.feature.schedule.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.core.money.UNSPECIFIED_CURRENCY
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

@Entity(
    tableName = "scheduled_transactions_table",
    indices = [Index(value = ["ownerId", "firestoreId"], unique = true)]
)
data class ScheduledTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreId: String = "",
    val amountMinor: Long = 0,
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

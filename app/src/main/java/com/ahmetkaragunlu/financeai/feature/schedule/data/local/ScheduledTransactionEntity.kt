package com.ahmetkaragunlu.financeai.feature.schedule.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

@Entity(tableName = "scheduled_transactions_table")
data class ScheduledTransactionEntity(
    @PrimaryKey(autoGenerate = true)
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
    val syncedToFirebase: Boolean = false
)

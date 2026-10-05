package com.ahmetkaragunlu.financeai.feature.transaction.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

@Entity(tableName = "transaction_table", indices = [Index(value = ["ownerId", "firestoreId"], unique = true)])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firestoreId: String = "",
    val amountMinor: Long = 0,
    val transaction: TransactionType,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val category: CategoryType,
    val photoUri: String? = null,
    val locationFull: String? = null,
    val locationShort: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ownerId: String = "",
    val currencyCode: String = "XXX",
    val syncedToFirebase: Boolean = false
)

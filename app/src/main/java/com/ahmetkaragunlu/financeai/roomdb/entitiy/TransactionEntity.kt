package com.ahmetkaragunlu.financeai.roomdb.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType

@Entity(tableName = "transaction_table")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
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
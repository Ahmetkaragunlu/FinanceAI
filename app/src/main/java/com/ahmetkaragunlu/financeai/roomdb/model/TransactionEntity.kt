package com.ahmetkaragunlu.financeai.roomdb.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType

@Entity(tableName = "transaction_table")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val amount : Double = 0.0,
    val transaction : TransactionType,
    val note : String = "",
    val date : Long = System.currentTimeMillis(),
    val category: CategoryType
)





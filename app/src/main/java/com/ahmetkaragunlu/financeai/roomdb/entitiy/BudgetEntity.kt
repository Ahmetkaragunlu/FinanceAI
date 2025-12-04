package com.ahmetkaragunlu.financeai.roomdb.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType

@Entity(tableName = "budget_table")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firestoreId: String = "",
    val budgetType: BudgetType,
    val category: CategoryType? = null,
    val amount: Double = 0.0,
    val limitPercentage: Double? = null,
    val syncedToFirebase: Boolean = false
)
package com.ahmetkaragunlu.financeai.feature.budget.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

@Entity(tableName = "budget_table", indices = [Index(value = ["ownerId", "firestoreId"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firestoreId: String = "",
    val budgetType: BudgetType,
    val category: CategoryType? = null,
    val amountMinor: Long = 0,
    val limitPercentage: Double? = null,
    val ownerId: String = "",
    val currencyCode: String = "XXX",
    val syncedToFirebase: Boolean = false
)

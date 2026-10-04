package com.ahmetkaragunlu.financeai.feature.budget.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType

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

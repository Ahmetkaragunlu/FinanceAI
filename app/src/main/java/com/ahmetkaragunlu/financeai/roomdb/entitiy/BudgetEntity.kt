package com.ahmetkaragunlu.financeai.roomdb.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType

@Entity(tableName = "budget_table")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firestoreId: String = "", // İleride sync için kullanılacak

    val budgetType: BudgetType,

    // Eğer GENERAL ise bu null olabilir. Kategori kuralları için dolu olmalı.
    val category: CategoryType? = null,

    // Sabit tutar (Genel Bütçe veya Kategori Tutar için)
    val amount: Double = 0.0,

    // Yüzdelik dilim (Sadece CATEGORY_PERCENTAGE ise dolu olur, örn: 10.0)
    val limitPercentage: Double? = null,

    val syncedToFirebase: Boolean = false
)
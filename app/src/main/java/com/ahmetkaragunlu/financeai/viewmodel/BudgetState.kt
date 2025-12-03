package com.ahmetkaragunlu.financeai.viewmodel

import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType

data class BudgetUiState(

    // Genel Bütçe Kartı Verisi
    val generalBudgetState: GeneralBudgetState? = null,

    // Kategori Listesi Verisi
    val categoryBudgetStates: List<CategoryBudgetState> = emptyList(),

    // AI Uyarı Mesajı (Boşsa kart görünmez)
    val aiWarningMessage: String? = null,

    // Hiç bütçe kuralı yoksa true olur -> Boş Tasarım (AnalysisScreen) gösterilir
    val isBudgetEmpty: Boolean = false
)

data class GeneralBudgetState(
    val limitAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progress: Float, // 0.0 ile 1.0 arası
    val incomeAmount: Double, // Karttaki Yeşil + Gelir
    val expenseAmount: Double // Karttaki Kırmızı - Gider
)

data class CategoryBudgetState(
    val category: CategoryType,
    val budgetType: BudgetType,
    val limitAmount: Double, // Yüzde ise hesaplanmış tutar buraya gelir
    val spentAmount: Double,
    val progress: Float,
    val isOverBudget: Boolean,
    val percentageUsed: Int // Ekranda %110 yazan kısım
)
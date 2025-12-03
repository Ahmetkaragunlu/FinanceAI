package com.ahmetkaragunlu.financeai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    // Bu ayın tarih aralığı
    private val currentMonthRange = DateFormatter.getCurrentMonthRange()

    // 1. Akış: Tüm Bütçe Kuralları
    private val budgetRulesFlow = budgetRepository.getAllBudgets()

    // 2. Akış: Bu ayın toplam GELİRİ (Karttaki yeşil alan için)
    private val totalIncomeFlow = financeRepository.getTotalIncomeByDateRange(
        currentMonthRange.first, currentMonthRange.second
    )

    // 3. Akış: Bu ayın toplam GİDERİ (Genel bütçe hesaplaması için)
    private val totalExpenseFlow = financeRepository.getTotalExpenseByDateRange(
        currentMonthRange.first, currentMonthRange.second
    )

    // 4. Akış: Kategori bazlı harcamalar (Kategori kartları için)
    private val categoryExpensesFlow = financeRepository.getCategoryByTypeAndDateRange(
        TransactionType.EXPENSE, currentMonthRange.first, currentMonthRange.second
    )

    // Tüm akışları birleştirip UI State oluşturuyoruz
    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRulesFlow,
        totalIncomeFlow,
        totalExpenseFlow,
        categoryExpensesFlow
    ) { rules, totalIncome, totalExpense, categoryExpenses ->

        // Eğer hiç kural yoksa direkt boş state dön
        if (rules.isEmpty()) {
            return@combine BudgetUiState(isBudgetEmpty = true)
        }

        // --- 1. GENEL BÜTÇE HESAPLAMASI ---
        val generalRule = rules.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
        val generalBudgetState = generalRule?.let { rule ->
            val limit = rule.amount
            val spent = totalExpense ?: 0.0
            val remaining = limit - spent
            val progress = (spent / limit).toFloat().coerceIn(0f, 1f)

            GeneralBudgetState(
                limitAmount = limit,
                spentAmount = spent,
                remainingAmount = remaining,
                progress = progress,
                incomeAmount = totalIncome ?: 0.0,
                expenseAmount = spent
            )
        }

        // --- 2. KATEGORİ BÜTÇELERİ HESAPLAMASI ---
        val categoryBudgetStates = rules.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }
            .map { rule ->
                // Bu kuralın kategorisine ait harcamayı bul
// Enum'ın ismini (.name) alarak String'e çeviriyoruz
                val categorySpent = categoryExpenses.find { it.category == rule.category?.name }?.totalAmount ?: 0.0
                // Limiti hesapla (Tutar mı, Yüzde mi?)
                val limit = if (rule.budgetType == BudgetType.CATEGORY_PERCENTAGE && generalRule != null) {
                    // Yüzde kuralı: Genel bütçenin %X'i
                    generalRule.amount * ((rule.limitPercentage ?: 0.0) / 100)
                } else {
                    // Sabit tutar kuralı
                    rule.amount
                }

                val progress = if (limit > 0) (categorySpent / limit).toFloat() else 0f
                val isOverBudget = categorySpent > limit
                val percentageUsed = if (limit > 0) ((categorySpent / limit) * 100).toInt() else 0

                CategoryBudgetState(
                    category = rule.category ?: CategoryType.OTHER,
                    budgetType = rule.budgetType,
                    limitAmount = limit,
                    spentAmount = categorySpent,
                    progress = progress.coerceIn(0f, 1f), // Bar 1.0'ı geçmesin görsel olarak
                    isOverBudget = isOverBudget,
                    percentageUsed = percentageUsed
                )
            }.sortedByDescending { it.percentageUsed } // En çok dolanları üste al

        // --- 3. AI UYARI MESAJI HAZIRLIĞI ---
        // Burada basit bir kural tabanlı uyarı oluşturuyoruz. İleride Gemini API bağlayacağız.
        val warningMessage = generateLocalWarning(categoryBudgetStates, generalBudgetState)

        BudgetUiState(
            isBudgetEmpty = false,
            generalBudgetState = generalBudgetState,
            categoryBudgetStates = categoryBudgetStates,
            aiWarningMessage = warningMessage
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState()
    )

    // Basit bir yerel uyarı mekanizması (Gemini entegrasyonuna kadar placeholder)
    private fun generateLocalWarning(
        categories: List<CategoryBudgetState>,
        general: GeneralBudgetState?
    ): String? {
        val overBudgetCount = categories.count { it.isOverBudget }
        val criticalCount = categories.count { !it.isOverBudget && it.progress > 0.85f }

        return when {
            overBudgetCount > 0 -> "Dikkat! $overBudgetCount kategoride limitini aştın. Harcamalarını gözden geçir."
            general != null && general.progress > 0.9f -> "Genel bütçenin sonuna yaklaştın (%${(general.progress * 100).toInt()})."
            criticalCount > 0 -> "$criticalCount kategori limit sınırına çok yakın."
            else -> null // Her şey yolunda, uyarı yok
        }
    }

    // Yeni Limit Ekleme Fonksiyonu (UI'dan çağrılacak)
    fun addBudgetRule(
        type: BudgetType,
        amount: Double,
        category: CategoryType?,
        percentage: Double? = null
    ) {
        viewModelScope.launch {
            val entity = BudgetEntity(
                budgetType = type,
                amount = amount,
                category = category,
                limitPercentage = percentage,
                firestoreId = "", // Sync servisi bunu daha sonra dolduracak
                syncedToFirebase = false
            )
            budgetRepository.insertBudget(entity)
            // Not: Insert sonrası Flow otomatik tetiklenir ve UI güncellenir.
        }
    }
}
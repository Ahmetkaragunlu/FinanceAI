package com.ahmetkaragunlu.financeai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import com.ahmetkaragunlu.financeai.utils.toResId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class AiSuggestionState(
    val messageText: String = "",
    val aiPrompt: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    val repository: FinanceRepository,
    private val budgetRepository: BudgetRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val lastMonthDateRange = DateFormatter.getDateRange(R.string.last_month)
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    private fun Flow<Double?>.toFormattedCurrency(): StateFlow<String> =
        this.map { currencyFormat.format(it ?: 0.0) }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = currencyFormat.format(0.0)
            )

    val lastMonthIncomeFormatted: StateFlow<String> =
        repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
            .toFormattedCurrency()

    val lastMonthExpenseFormatted: StateFlow<String> =
        repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
            .toFormattedCurrency()

    val lastMonthRemainingBalance: StateFlow<Double> =
        combine(
            repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
            repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
        ) { income, expense ->
            (income ?: 0.0) - (expense ?: 0.0)
        }.distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val lastMonthRemainingBalanceFormatted: StateFlow<String> =
        lastMonthRemainingBalance.toFormattedCurrency()

    val spendingPercentage: StateFlow<Double> =
        combine(
            repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
            repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
        ) { income, expense ->
            val incomeValue = income ?: 0.0
            val expenseValue = expense ?: 0.0
            if (incomeValue > 0) {
                (incomeValue - expenseValue) / incomeValue
            } else {
                0.0
            }
        }.distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val homeUiState: StateFlow<HomeUiState> = combine(
        lastMonthIncomeFormatted,
        lastMonthExpenseFormatted,
        lastMonthRemainingBalance,
        lastMonthRemainingBalanceFormatted,
        spendingPercentage
    ) { income, expense, balance, balanceFormatted, percentage ->
        HomeUiState(
            totalIncome = income,
            totalExpense = expense,
            remainingBalance = balance,
            remainingBalanceFormatted = balanceFormatted,
            spendingPercentage = percentage
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    val lastMonthCategoryExpenses: StateFlow<List<CategoryExpense>> =
        repository.getCategoryByTypeAndDateRange(
            transactionType = TransactionType.EXPENSE,
            startDate = lastMonthDateRange.first,
            endDate = lastMonthDateRange.second
        ).distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ✨ GÜNCEL: AI Önerisi State
    val aiSuggestion: StateFlow<AiSuggestionState> = combine(
        budgetRepository.getAllBudgets(),
        repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
        lastMonthCategoryExpenses
    ) { budgets, totalExpense, categoryExpenses ->
        generateAiSuggestion(budgets, totalExpense ?: 0.0, categoryExpenses)
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiSuggestionState(
            messageText = "Finansal durumunu analiz etmek için tıkla",
            aiPrompt = ""
        )
    )

    private fun generateAiSuggestion(
        budgets: List<com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity>,
        totalExpense: Double,
        categoryExpenses: List<CategoryExpense>
    ): AiSuggestionState {
        // 1. Bütçe yoksa genel öneri
        if (budgets.isEmpty()) {
            return if (totalExpense > 0) {
                AiSuggestionState(
                    messageText = "Harcamaların artıyor! Bütçe limiti oluşturmak için tıkla",
                    aiPrompt = "Bu ay toplam ${currencyFormat.format(totalExpense)} harcama yaptım. Kendime uygun bir bütçe limiti belirlememe ve tasarruf etmeme yardımcı olur musun?"
                )
            } else {
                AiSuggestionState(
                    messageText = "Finansal hedeflerine ulaşmak ve planlama yapmak için tıkla",
                    aiPrompt = "Henüz harcama yapmadım ama finansal planlama yapmak istiyorum. Bana nasıl bir yol haritası önerirsin?"
                )
            }
        }

        // 2. Genel bütçe kontrolü
        val generalBudget = budgets.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
        generalBudget?.let { budget ->
            val limit = budget.amount
            val percentage = if (limit > 0) (totalExpense / limit) * 100 else 0.0

            if (totalExpense > limit) {
                val overflowAmount = totalExpense - limit
                return AiSuggestionState(
                    messageText = "Dikkat! Bütçeni ${currencyFormat.format(overflowAmount)} aştın. Tasarruf planı için tıkla",
                    aiPrompt = "Aylık bütçem ${currencyFormat.format(limit)} idi ancak şu an ${currencyFormat.format(totalExpense)} harcadım. Bütçemi %${percentage.toInt()} oranında aştım. Durumu toparlamak için acil tasarruf önerilerin neler?"
                )
            } else if (percentage >= 80) {
                return AiSuggestionState(
                    messageText = "Genel bütçenin %${percentage.toInt()}'ine ulaştın. Ay sonunu getirmek için tıkla",
                    aiPrompt = "Aylık bütçemin %${percentage.toInt()}'ini şimdiden harcadım. Ayın geri kalanında bakiyemi korumak için nelere dikkat etmeliyim?"
                )
            }
        }

        // 3. Kategori bütçe kontrolü
        val categoryBudgets = budgets.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }
        categoryBudgets.forEach { budget ->
            val categoryName = budget.category?.name
            val spent = categoryExpenses.find { it.category == categoryName }?.totalAmount ?: 0.0
            val limit = if (budget.budgetType == BudgetType.CATEGORY_PERCENTAGE && generalBudget != null) {
                generalBudget.amount * ((budget.limitPercentage ?: 0.0) / 100)
            } else {
                budget.amount
            }

            val percentage = if (limit > 0) (spent / limit) * 100 else 0.0
            val catNameRes = budget.category!!.toResId() // String Resource ID
            val catName = context.getString(catNameRes)

            if (spent > limit) {
                return AiSuggestionState(
                    messageText = "$catName bütçeni aştın! Tasarruf için tıkla",
                    aiPrompt = "$catName kategorisinde belirlediğim limiti aştım. (${currencyFormat.format(limit)} limit, ${currencyFormat.format(spent)} harcama). Bu kategoride neden bu kadar harcama yapmış olabilirim ve nasıl kısabilirim?"
                )
            } else if (percentage >= 80) {
                return AiSuggestionState(
                    messageText = "$catName harcamaların sınıra yaklaştı (%${percentage.toInt()}). Önlem almak için tıkla",
                    aiPrompt = "$catName kategorisinde harcama limitimin %${percentage.toInt()}'ine ulaştım. Bu kategoride daha fazla harcama yapmamak için önerilerin var mı?"
                )
            }
        }

        // 4. Her şey yolundaysa
        return AiSuggestionState(
            messageText = "Bütçen gayet sağlıklı görünüyor! Detaylı analiz için tıkla",
            aiPrompt = "Şu ana kadar harcamalarım bütçe planıma uygun gidiyor. Finansal durumumu daha da iyileştirmek için yatırım veya birikim tavsiyesi verebilir misin?"
        )
    }
}
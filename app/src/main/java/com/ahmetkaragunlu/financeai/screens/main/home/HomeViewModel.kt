package com.ahmetkaragunlu.financeai.screens.main.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.firebaserepo.AuthRepository
import com.ahmetkaragunlu.financeai.roomdb.entitiy.BudgetEntity
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import com.ahmetkaragunlu.financeai.roomrepository.budgetrepositroy.BudgetRepository
import com.ahmetkaragunlu.financeai.roomrepository.financerepository.FinanceRepository
import com.ahmetkaragunlu.financeai.utils.DateFormatter
import com.ahmetkaragunlu.financeai.utils.formatAsCurrency
import com.ahmetkaragunlu.financeai.utils.toResId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val repository: FinanceRepository,
    private val budgetRepository: BudgetRepository,
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        private const val BUDGET_WARNING_THRESHOLD = 80.0
        private const val FLOW_TIMEOUT = 5_000L
    }

    private val lastMonthDateRange = DateFormatter.getDateRange(R.string.last_month)

    var showLogoutDialog by mutableStateOf(false)

    val userName: StateFlow<String> = flow {
        val name = authRepository.getUserName()
        emit(name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
        initialValue = ""
    )

    private fun Flow<Double?>.toFormattedCurrency(): StateFlow<String> =
        this.map { (it ?: 0.0).formatAsCurrency() }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
                initialValue = 0.0.formatAsCurrency()
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
            started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
            initialValue = 0.0
        )

    val lastMonthRemainingBalanceFormatted: StateFlow<String> =
        lastMonthRemainingBalance.toFormattedCurrency()

    val spendingPercentage: StateFlow<Double> =
        combine(
            repository.getTotalIncomeByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
            repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second)
        ) { income, expense ->
            calculateSpendingPercentage(income ?: 0.0, expense ?: 0.0)
        }.distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
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
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
        initialValue = HomeUiState()
    )

    val lastMonthCategoryExpenses: StateFlow<List<CategoryExpense>> =
        repository.getCategoryByTypeAndDateRange(
            transactionType = TransactionType.EXPENSE,
            startDate = lastMonthDateRange.first,
            endDate = lastMonthDateRange.second
        ).distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
            initialValue = emptyList()
        )

    val aiSuggestion: StateFlow<AiSuggestionState> = combine(
        budgetRepository.getAllBudgets(),
        repository.getTotalExpenseByDateRange(lastMonthDateRange.first, lastMonthDateRange.second),
        lastMonthCategoryExpenses
    ) { budgets, totalExpense, categoryExpenses ->
        generateAiSuggestion(budgets, totalExpense ?: 0.0, categoryExpenses)
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
        initialValue = AiSuggestionState(
            messageText = "Finansal durumunu analiz etmek için tıkla",
            aiPrompt = ""
        )
    )

    private fun calculateSpendingPercentage(income: Double, expense: Double): Double {
        return if (income > 0) (income - expense) / income else 0.0
    }

    private fun calculatePercentage(spent: Double, limit: Double): Double {
        return if (limit > 0) (spent / limit) * 100 else 0.0
    }

    private fun generateAiSuggestion(
        budgets: List<BudgetEntity>,
        totalExpense: Double,
        categoryExpenses: List<CategoryExpense>
    ): AiSuggestionState {
        if (budgets.isEmpty()) {
            return getNoBudgetSuggestion(totalExpense)
        }

        val generalBudget = budgets.find { it.budgetType == BudgetType.GENERAL_MONTHLY }
        generalBudget?.let { budget ->
            val generalBudgetSuggestion = checkGeneralBudget(budget, totalExpense)
            if (generalBudgetSuggestion != null) return generalBudgetSuggestion
        }

        val categoryBudgets = budgets.filter { it.budgetType != BudgetType.GENERAL_MONTHLY }
        val categoryBudgetSuggestion = checkCategoryBudgets(
            categoryBudgets,
            categoryExpenses,
            generalBudget
        )
        if (categoryBudgetSuggestion != null) return categoryBudgetSuggestion

        return getHealthyBudgetSuggestion()
    }

    private fun getNoBudgetSuggestion(totalExpense: Double): AiSuggestionState {
        return if (totalExpense > 0) {
            AiSuggestionState(
                messageText = "Harcamaların artıyor! Bütçe limiti oluşturmak için tıkla",
                aiPrompt = "Bu ay toplam ${totalExpense.formatAsCurrency()} harcama yaptım. Kendime uygun bir bütçe limiti belirlememe ve tasarruf etmeme yardımcı olur musun?"
            )
        } else {
            AiSuggestionState(
                messageText = "Finansal hedeflerine ulaşmak ve planlama yapmak için tıkla",
                aiPrompt = "Henüz harcama yapmadım ama finansal planlama yapmak istiyorum. Bana nasıl bir yol haritası önerirsin?"
            )
        }
    }

    private fun checkGeneralBudget(
        budget: BudgetEntity,
        totalExpense: Double
    ): AiSuggestionState? {
        val limit = budget.amount
        val percentage = calculatePercentage(totalExpense, limit)

        return when {
            totalExpense > limit -> {
                val overflowAmount = totalExpense - limit
                AiSuggestionState(
                    messageText = "Dikkat! Bütçeni ${overflowAmount.formatAsCurrency()} aştın. Tasarruf planı için tıkla",
                    aiPrompt = "Aylık bütçem ${limit.formatAsCurrency()} idi ancak şu an ${totalExpense.formatAsCurrency()} harcadım. Bütçemi %${percentage.toInt()} oranında aştım. Durumu toparlamak için acil tasarruf önerilerin neler?"
                )
            }
            percentage >= BUDGET_WARNING_THRESHOLD -> {
                AiSuggestionState(
                    messageText = "Genel bütçenin %${percentage.toInt()}'ine ulaştın. Ay sonunu getirmek için tıkla",
                    aiPrompt = "Aylık bütçemin %${percentage.toInt()}'ini şimdiden harcadım. Ayın geri kalanında bakiyemi korumak için nelere dikkat etmeliyim?"
                )
            }
            else -> null
        }
    }

    private fun checkCategoryBudgets(
        categoryBudgets: List<BudgetEntity>,
        categoryExpenses: List<CategoryExpense>,
        generalBudget: BudgetEntity?
    ): AiSuggestionState? {
        categoryBudgets.forEach { budget ->
            val categoryName = budget.category?.name
            val spent = categoryExpenses.find { it.category == categoryName }?.totalAmount ?: 0.0
            val limit = calculateCategoryLimit(budget, generalBudget)
            val percentage = calculatePercentage(spent, limit)
            val catName = context.getString(budget.category!!.toResId())

            when {
                spent > limit -> {
                    return AiSuggestionState(
                        messageText = "$catName bütçeni aştın! Tasarruf için tıkla",
                        aiPrompt = "$catName kategorisinde belirlediğim limiti aştım. (${limit.formatAsCurrency()} limit, ${spent.formatAsCurrency()} harcama). Bu kategoride neden bu kadar harcama yapmış olabilirim ve nasıl kısabilirim?"
                    )
                }
                percentage >= BUDGET_WARNING_THRESHOLD -> {
                    return AiSuggestionState(
                        messageText = "$catName harcamaların sınıra yaklaştı (%${percentage.toInt()}). Önlem almak için tıkla",
                        aiPrompt = "$catName kategorisinde harcama limitimin %${percentage.toInt()}'ine ulaştım. Bu kategoride daha fazla harcama yapmamak için önerilerin var mı?"
                    )
                }
            }
        }
        return null
    }

    private fun calculateCategoryLimit(budget: BudgetEntity, generalBudget: BudgetEntity?): Double {
        return if (budget.budgetType == BudgetType.CATEGORY_PERCENTAGE && generalBudget != null) {
            generalBudget.amount * ((budget.limitPercentage ?: 0.0) / 100)
        } else {
            budget.amount
        }
    }

    private fun getHealthyBudgetSuggestion(): AiSuggestionState {
        return AiSuggestionState(
            messageText = "Bütçen gayet sağlıklı görünüyor! Detaylı analiz için tıkla",
            aiPrompt = "Şu ana kadar harcamalarım bütçe planıma uygun gidiyor. Finansal durumumu daha da iyileştirmek için yatırım veya birikim tavsiyesi verebilir misin?"
        )
    }
}
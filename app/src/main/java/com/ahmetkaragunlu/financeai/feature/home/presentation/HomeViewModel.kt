package com.ahmetkaragunlu.financeai.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.core.format.formatAsCurrency
import com.ahmetkaragunlu.financeai.core.session.AccountSession
import com.ahmetkaragunlu.financeai.core.time.FinanceCalendar
import com.ahmetkaragunlu.financeai.feature.auth.domain.repository.AuthRepository
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateBudgetUsagePercentage
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.calculateCategoryBudgetLimit
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.domain.repository.BudgetRepository
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.repository.TransactionRepository
import com.ahmetkaragunlu.financeai.feature.transaction.format.toResId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val calendar: FinanceCalendar,
    private val session: AccountSession,
    val repository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : ViewModel() {

    companion object {
        private const val BUDGET_WARNING_THRESHOLD = 80.0
        private const val FLOW_TIMEOUT = 5_000L
    }

    private val month = calendar.observeMonth()

    val userName: StateFlow<String> = flow {
        val name = authRepository.getUserName()
        emit(name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
        initialValue = ""
    )

    val homeUiState: StateFlow<HomeUiState> = month.flatMapLatest {
        repository.observeFinancialSummary(it.start, it.endExclusive)
    }.map { summary ->
        val currency = session.account.value?.currencyCode ?: "XXX"
        HomeUiState(
            totalIncome = summary.income.formatAsCurrency(currency),
            totalExpense = summary.expense.formatAsCurrency(currency),
            remainingBalance = summary.remainingBalance,
            remainingBalanceFormatted = summary.remainingBalance.formatAsCurrency(currency),
            remainingIncomeRatio = summary.remainingIncomeRatio
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
        initialValue = HomeUiState()
    )

    val monthlyCategoryExpenses: StateFlow<List<CategoryExpense>> =
        month.flatMapLatest { repository.observeCategoryExpensesByTypeAndDateRange(TransactionType.EXPENSE, it.start, it.endExclusive) }.distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FLOW_TIMEOUT),
            initialValue = emptyList()
        )

    val aiSuggestion: StateFlow<AiSuggestionState> = combine(
        budgetRepository.observeBudgets(),
        month.flatMapLatest { repository.observeTotalExpenseByDateRange(it.start, it.endExclusive) },
        month.flatMapLatest { repository.observeCategoryExpensesByTypeAndDateRange(TransactionType.EXPENSE, it.start, it.endExclusive) }
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

    private fun generateAiSuggestion(
        budgets: List<Budget>,
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
                aiPrompt = "Bu ay toplam ${totalExpense.formatAsCurrency(session.account.value?.currencyCode ?: "XXX")} harcama yaptım. Kendime uygun bir bütçe limiti belirlememe ve tasarruf etmeme yardımcı olur musun?"
            )
        } else {
            AiSuggestionState(
                messageText = "Finansal hedeflerine ulaşmak ve planlama yapmak için tıkla",
                aiPrompt = "Henüz harcama yapmadım ama finansal planlama yapmak istiyorum. Bana nasıl bir yol haritası önerirsin?"
            )
        }
    }

    private fun checkGeneralBudget(
        budget: Budget,
        totalExpense: Double
    ): AiSuggestionState? {
        val limit = budget.amount
        val percentage = calculateBudgetUsagePercentage(totalExpense, limit)

        return when {
            totalExpense > limit -> {
                val overflowAmount = totalExpense - limit
                AiSuggestionState(
                    messageText = "Dikkat! Bütçeni ${overflowAmount.formatAsCurrency(session.account.value?.currencyCode ?: "XXX")} aştın. Tasarruf planı için tıkla",
                    aiPrompt = "Aylık bütçem ${limit.formatAsCurrency(session.account.value?.currencyCode ?: "XXX")} idi ancak şu an ${totalExpense.formatAsCurrency(session.account.value?.currencyCode ?: "XXX")} harcadım. Bütçemi %${percentage.toInt()} oranında aştım. Durumu toparlamak için acil tasarruf önerilerin neler?"
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
        categoryBudgets: List<Budget>,
        categoryExpenses: List<CategoryExpense>,
        generalBudget: Budget?
    ): AiSuggestionState? {
        categoryBudgets.forEach { budget ->
            val categoryName = budget.category?.name
            val spent = categoryExpenses.find { it.category == categoryName }?.totalAmount ?: 0.0
            val limit = calculateCategoryBudgetLimit(budget, generalBudget)
            val percentage = calculateBudgetUsagePercentage(spent, limit)
            val catName = context.getString(budget.category!!.toResId())

            when {
                spent > limit -> {
                    return AiSuggestionState(
                        messageText = "$catName bütçeni aştın! Tasarruf için tıkla",
                        aiPrompt = "$catName kategorisinde belirlediğim limiti aştım. (${limit.formatAsCurrency(session.account.value?.currencyCode ?: "XXX")} limit, ${spent.formatAsCurrency(session.account.value?.currencyCode ?: "XXX")} harcama). Bu kategoride neden bu kadar harcama yapmış olabilirim ve nasıl kısabilirim?"
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

    private fun getHealthyBudgetSuggestion(): AiSuggestionState {
        return AiSuggestionState(
            messageText = "Bütçen gayet sağlıklı görünüyor! Detaylı analiz için tıkla",
            aiPrompt = "Şu ana kadar harcamalarım bütçe planıma uygun gidiyor. Finansal durumumu daha da iyileştirmek için yatırım veya birikim tavsiyesi verebilir misin?"
        )
    }
}

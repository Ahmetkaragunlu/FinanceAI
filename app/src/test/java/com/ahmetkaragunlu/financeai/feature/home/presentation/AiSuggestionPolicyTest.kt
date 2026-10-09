package com.ahmetkaragunlu.financeai.feature.home.presentation

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class AiSuggestionPolicyTest {
    @Test fun unknownCategorySpendingDoesNotBecomeAnOtherCategoryBudgetWarning() {
        val budgets = listOf(Budget(budgetType = BudgetType.CATEGORY_AMOUNT, category = CategoryType.OTHER, amount = 10.0))
        assertEquals(AiSuggestionState.Healthy, buildAiSuggestion(budgets, 20.0, listOf(CategoryExpense(null, 20.0))))
        assertEquals(AiSuggestionState.CategoryNearLimit(CategoryType.OTHER, 80),
            buildAiSuggestion(budgets, 8.0, listOf(CategoryExpense(CategoryType.OTHER, 8.0))))
    }
    @Test
    fun noBudgetUsesActualSpendingOrPlanningWithoutInventingExpenses() {
        assertEquals(AiSuggestionState.Planning, buildAiSuggestion(emptyList(), 0.0, emptyList()))
        assertEquals(
            AiSuggestionState.NoBudget(25.0),
            buildAiSuggestion(emptyList(), 25.0, emptyList()),
        )
    }

    @Test
    fun generalWarningThresholdAndOverflowKeepTheirExistingMeaning() {
        val budgets = listOf(Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0))
        assertEquals(AiSuggestionState.Healthy, buildAiSuggestion(budgets, 79.0, emptyList()))
        assertEquals(
            AiSuggestionState.GeneralNearLimit(80),
            buildAiSuggestion(budgets, 80.0, emptyList()),
        )
        assertEquals(
            AiSuggestionState.GeneralNearLimit(100),
            buildAiSuggestion(budgets, 100.0, emptyList()),
        )
        assertEquals(
            AiSuggestionState.GeneralExceeded(100.0, 125.0, 125),
            buildAiSuggestion(budgets, 125.0, emptyList()),
        )
    }

    @Test
    fun generalBudgetWarningStillTakesPriorityOverACategoryWarning() {
        val budgets =
            listOf(
                Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0),
                Budget(
                    budgetType = BudgetType.CATEGORY_AMOUNT,
                    category = CategoryType.FOOD,
                    amount = 10.0,
                ),
            )
        assertEquals(
            AiSuggestionState.GeneralNearLimit(85),
            buildAiSuggestion(budgets, 85.0, listOf(CategoryExpense(CategoryType.FOOD, 30.0))),
        )
    }

    @Test
    fun percentageCategoryUsesTheGeneralBudgetAndPreservesTheCategoryIdentity() {
        val budgets =
            listOf(
                Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0),
                Budget(
                    budgetType = BudgetType.CATEGORY_PERCENTAGE,
                    category = CategoryType.FOOD,
                    limitPercentage = 20.0,
                ),
            )
        assertEquals(
            AiSuggestionState.CategoryExceeded(CategoryType.FOOD, 20.0, 25.0),
            buildAiSuggestion(budgets, 25.0, listOf(CategoryExpense(CategoryType.FOOD, 25.0))),
        )
    }
}

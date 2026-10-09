package com.ahmetkaragunlu.financeai.feature.budget.presentation

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetWarningTest {
    private fun category(category: CategoryType, over: Boolean = true) = CategoryBudgetState(
        id = category.ordinal, category = category, budgetType = BudgetType.CATEGORY_AMOUNT,
        limitAmount = 100.0, spentAmount = if (over) 110.0 else 20.0,
        progress = if (over) 1f else 0.2f, isOverBudget = over, percentageUsed = if (over) 110 else 20,
    )
    private fun general(remaining: Double, progress: Float) = GeneralBudgetState(
        id = 1, limitAmount = 100.0, spentAmount = 100.0 - remaining,
        remainingAmount = remaining, progress = progress, incomeAmount = 100.0, expenseAmount = 100.0 - remaining,
    )

    @Test fun generalOverrunRetainsPriorityAndSpecificCategoryOrCount() {
        val general = general(-10.0, 1.1f)
        assertEquals(BudgetWarning.GeneralExceeded, budgetWarning(emptyList(), general))
        assertEquals(BudgetWarning.GeneralAndCategoryExceeded(CategoryType.FOOD),
            budgetWarning(listOf(category(CategoryType.FOOD)), general))
        assertEquals(BudgetWarning.GeneralAndCategoriesExceeded(2),
            budgetWarning(listOf(category(CategoryType.FOOD), category(CategoryType.GROCERIES)), general))
    }

    @Test fun categoryOverrunRetainsItsCategoryOrCountWithoutGeneralOverrun() {
        assertEquals(BudgetWarning.CategoryExceeded(CategoryType.FOOD),
            budgetWarning(listOf(category(CategoryType.FOOD)), general(10.0, 0.9f)))
        assertEquals(BudgetWarning.CategoriesExceeded(2),
            budgetWarning(listOf(category(CategoryType.FOOD), category(CategoryType.GROCERIES)), null))
    }

    @Test fun nearLimitThresholdAndHealthyOrMissingBudgetDoNotChange() {
        assertNull(budgetWarning(emptyList(), null))
        assertNull(budgetWarning(listOf(category(CategoryType.FOOD, false)), general(20.01, 0.7999f)))
        assertEquals(BudgetWarning.GeneralNearLimit, budgetWarning(emptyList(), general(20.0, 0.8f)))
        assertEquals(BudgetWarning.GeneralNearLimit, budgetWarning(emptyList(), general(19.99, 0.8001f)))
        assertNull(budgetWarning(emptyList(), general(0.0, 0.5f)))
    }
}

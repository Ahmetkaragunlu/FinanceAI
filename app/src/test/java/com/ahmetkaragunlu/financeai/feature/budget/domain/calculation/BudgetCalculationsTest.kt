package com.ahmetkaragunlu.financeai.feature.budget.domain.calculation

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculationsTest {
    @Test
    fun `percentage limit uses the general budget and preserves fractional percentages`() {
        val general = Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 1_250.0)
        val category = Budget(budgetType = BudgetType.CATEGORY_PERCENTAGE,
            amount = 17.0, limitPercentage = 12.5)

        assertEquals(156.25, calculateCategoryBudgetLimit(category, general), 0.0)
    }

    @Test
    fun `missing general budget keeps the existing stored amount fallback`() {
        val category = Budget(budgetType = BudgetType.CATEGORY_PERCENTAGE,
            amount = 17.5, limitPercentage = 12.5)

        assertEquals(17.5, calculateCategoryBudgetLimit(category, null), 0.0)
    }

    @Test
    fun `missing percentage with general budget keeps the existing zero limit`() {
        val general = Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 1_250.0)
        val category = Budget(budgetType = BudgetType.CATEGORY_PERCENTAGE, amount = 17.0)

        assertEquals(0.0, calculateCategoryBudgetLimit(category, general), 0.0)
    }

    @Test
    fun `fixed amount limit is not changed by a stored percentage or a general budget`() {
        val general = Budget(budgetType = BudgetType.GENERAL_MONTHLY, amount = 1_250.0)
        val category = Budget(budgetType = BudgetType.CATEGORY_AMOUNT,
            amount = 217.75, limitPercentage = 12.5)

        assertEquals(217.75, calculateCategoryBudgetLimit(category, general), 0.0)
    }

    @Test
    fun `usage keeps budget overflow instead of clamping the financial result`() {
        assertEquals(125.0, calculateBudgetUsagePercentage(250.0, 200.0), 0.0)
        assertEquals(12.5, calculateBudgetUsagePercentage(25.0, 200.0), 0.0)
    }

    @Test
    fun `nonpositive limit keeps the existing zero usage result`() {
        assertEquals(0.0, calculateBudgetUsagePercentage(250.0, 0.0), 0.0)
        assertEquals(0.0, calculateBudgetUsagePercentage(250.0, -200.0), 0.0)
    }
}

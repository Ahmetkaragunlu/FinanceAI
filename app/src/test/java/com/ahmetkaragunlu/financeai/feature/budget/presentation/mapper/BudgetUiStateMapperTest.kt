package com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetWarning
import com.ahmetkaragunlu.financeai.feature.budget.presentation.GeneralBudgetState
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetUiStateMapperTest {
    @Test
    fun generalAndCategoryProjectionsKeepLimitsWarningsAndUsageOrdering() {
        val rules = listOf(
            Budget(id = 1, budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0),
            Budget(id = 2, budgetType = BudgetType.CATEGORY_PERCENTAGE,
                category = CategoryType.FOOD, limitPercentage = 25.0),
            Budget(id = 3, budgetType = BudgetType.CATEGORY_AMOUNT,
                category = CategoryType.TRANSPORT, amount = 50.0),
        )
        val state = mapBudgetUiState(rules, 200.0, 80.0, listOf(
            CategoryExpense(CategoryType.FOOD, 40.0),
            CategoryExpense(CategoryType.TRANSPORT, 10.0),
        ))
        assertEquals(GeneralBudgetState(1, 100.0, 80.0, 20.0, 0.8f, 200.0, 80.0), state.generalBudgetState)
        assertEquals(listOf(2, 3), state.categoryBudgetStates.map { it.id })
        assertEquals(listOf(25.0, 50.0), state.categoryBudgetStates.map { it.limitAmount })
        assertEquals(listOf(160, 20), state.categoryBudgetStates.map { it.percentageUsed })
        assertEquals(listOf(1f, 0.2f), state.categoryBudgetStates.map { it.progress })
        assertEquals(BudgetWarning.CategoryExceeded(CategoryType.FOOD), state.warning)
        assertFalse(state.isBudgetEmpty)
    }

    @Test
    fun exceededGeneralProgressRemainsUnclampedAndItsWarningIsPreserved() {
        val state = mapBudgetUiState(
            listOf(Budget(id = 1, budgetType = BudgetType.GENERAL_MONTHLY, amount = 100.0)),
            50.0, 110.0, emptyList(),
        )
        assertEquals(1.1f, checkNotNull(state.generalBudgetState).progress)
        assertEquals(-10.0, checkNotNull(state.generalBudgetState).remainingAmount, 0.0)
        assertEquals(BudgetWarning.GeneralExceeded, state.warning)
    }

    @Test
    fun equalCategoryUsageRetainsInputOrderAndUnknownExpenseDoesNotBecomeOther() {
        val rules = listOf(
            Budget(id = 2, budgetType = BudgetType.CATEGORY_AMOUNT, category = CategoryType.TRANSPORT, amount = 50.0),
            Budget(id = 1, budgetType = BudgetType.CATEGORY_AMOUNT, category = CategoryType.FOOD, amount = 50.0),
            Budget(id = 3, budgetType = BudgetType.CATEGORY_AMOUNT, category = CategoryType.OTHER, amount = 10.0),
        )
        val state = mapBudgetUiState(rules, 0.0, 1_022.0, listOf(
            CategoryExpense(null, 1_000.0), CategoryExpense(CategoryType.TRANSPORT, 10.0),
            CategoryExpense(CategoryType.FOOD, 10.0), CategoryExpense(CategoryType.OTHER, 2.0),
        ))
        assertEquals(listOf(2, 1, 3), state.categoryBudgetStates.map { it.id })
        assertEquals(2.0, state.categoryBudgetStates.single { it.id == 3 }.spentAmount, 0.0)
        assertNull(state.generalBudgetState)
        assertNull(state.warning)
    }

    @Test
    fun zeroCategoryLimitKeepsItsExistingPercentageAndExceededDecision() {
        val state = mapBudgetUiState(listOf(
            Budget(id = 4, budgetType = BudgetType.CATEGORY_PERCENTAGE,
                category = CategoryType.FOOD, limitPercentage = 25.0),
        ), 0.0, 5.0, listOf(CategoryExpense(CategoryType.FOOD, 5.0)))
        val category = state.categoryBudgetStates.single()
        assertEquals(0.0, category.limitAmount, 0.0)
        assertEquals(0f, category.progress)
        assertEquals(0, category.percentageUsed)
        assertTrue(category.isOverBudget)
        assertEquals(BudgetWarning.CategoryExceeded(CategoryType.FOOD), state.warning)
    }
}

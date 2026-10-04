package com.ahmetkaragunlu.financeai.feature.budget.data.mapper

import com.ahmetkaragunlu.financeai.feature.budget.data.local.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMapperTest {
    @Test
    fun `general and category budgets retain their distinct rule fields`() {
        val general = BudgetEntity(
            id = 17, firestoreId = "general-budget", budgetType = BudgetType.GENERAL_MONTHLY,
            amount = 10_000.50, syncedToFirebase = true
        )
        val percentage = BudgetEntity(
            id = 18, firestoreId = "category-budget", budgetType = BudgetType.CATEGORY_PERCENTAGE,
            category = CategoryType.GROCERIES, amount = 0.0, limitPercentage = 15.5
        )
        val categoryAmount = percentage.copy(budgetType = BudgetType.CATEGORY_AMOUNT, amount = 250.25, limitPercentage = null)

        listOf(general, percentage, categoryAmount).forEach { row ->
            assertEquals(row, row.toDomain().toEntity())
        }
    }
}

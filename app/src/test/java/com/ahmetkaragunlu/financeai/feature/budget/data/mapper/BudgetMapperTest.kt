package com.ahmetkaragunlu.financeai.feature.budget.data.mapper

import com.ahmetkaragunlu.financeai.feature.budget.data.local.entity.BudgetEntity
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMapperTest {
    @Test
    fun `general and category budgets retain their distinct rule fields`() {
        val general = BudgetEntity(
            id = 17, firestoreId = "general-budget", budgetType = BudgetType.GENERAL_MONTHLY,
            currencyCode = "USD", amountMinor = 1000050L, syncedToFirebase = true
        )
        val percentage = BudgetEntity(
            id = 18, firestoreId = "category-budget", budgetType = BudgetType.CATEGORY_PERCENTAGE,
            category = CategoryType.GROCERIES, currencyCode = "USD", amountMinor = 0L, limitPercentage = 15.5
        )
        val categoryAmount = percentage.copy(budgetType = BudgetType.CATEGORY_AMOUNT, currencyCode = "USD", amountMinor = 25025L, limitPercentage = null)

        listOf(general, percentage, categoryAmount).forEach { row ->
            assertEquals(row, row.toDomain().toEntity())
        }
    }
}

package com.ahmetkaragunlu.financeai.feature.budget.data.remote

import com.ahmetkaragunlu.financeai.feature.budget.domain.model.Budget
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreBudgetMapperTest {
    @Test
    fun `general budget retains null category and fractional amount`() {
        val budget = Budget(id = 17, firestoreId = "remote-budget",
            budgetType = BudgetType.GENERAL_MONTHLY, currencyCode = "USD", amount = 1250.75)

        assertEquals(
            mapOf("budgetType" to "GENERAL_MONTHLY", "category" to null,
                "currencyCode" to "USD", "amountMinor" to 125075L, "amount" to 1250.75, "limitPercentage" to null),
            budget.toFirebaseMap()
        )
    }

    @Test
    fun `percentage budget retains percentage separately from the amount`() {
        val budget = Budget(budgetType = BudgetType.CATEGORY_PERCENTAGE,
            category = CategoryType.FOOD, currencyCode = "USD", amount = 0.0, limitPercentage = 12.5)

        assertEquals(
            mapOf("budgetType" to "CATEGORY_PERCENTAGE", "category" to "FOOD",
                "currencyCode" to "USD", "amountMinor" to 0L, "amount" to 0.0, "limitPercentage" to 12.5),
            budget.toFirebaseMap()
        )
    }
}

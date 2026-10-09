package com.ahmetkaragunlu.financeai.feature.transaction.data.mapper

import com.ahmetkaragunlu.financeai.feature.transaction.data.local.model.CategoryExpenseRow
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryExpenseMapperTest {
    @Test fun knownCategoriesKeepTheirMeaningAndCurrencyScale() {
        assertEquals(CategoryType.FOOD, CategoryExpenseRow("FOOD", 2550).toDomain("USD").category)
        assertEquals(25.50, CategoryExpenseRow("FOOD", 2550).toDomain("USD").totalAmount, 0.0)
        assertEquals(2550.0, CategoryExpenseRow("FOOD", 2550).toDomain("JPY").totalAmount, 0.0)
        assertEquals(CategoryType.OTHER, CategoryExpenseRow("OTHER", 1).toDomain("USD").category)
    }
    @Test fun unknownOrWrongCaseCategoriesAreNotSilentlyAddedToAnOtherBudget() {
        listOf("LEGACY", "food", "").forEach {
            val result = CategoryExpenseRow(it, 2550).toDomain("USD")
            assertNull(result.category)
            assertEquals(25.50, result.totalAmount, 0.0)
        }
    }
}

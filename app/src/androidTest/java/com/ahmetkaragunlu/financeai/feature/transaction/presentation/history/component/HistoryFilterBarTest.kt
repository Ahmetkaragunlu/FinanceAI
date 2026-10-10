package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.time.DateFilter
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.HistoryFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class HistoryFilterBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun categoryRequiresTypeAndTheMenusDispatchControlledDateTypeAndCategorySelections() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filters = mutableStateOf(HistoryFilters())
        val error = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                HistoryFilterBar(
                    filters = filters.value,
                    showCategoryError = error.value,
                    onDateSelected = { filters.value = filters.value.copy(date = it) },
                    onTypeSelected = {
                        filters.value = filters.value.copy(type = it, category = null)
                        error.value = false
                    },
                    onCategorySelected = { filters.value = filters.value.copy(category = it) },
                    onCategoryMenuRequested = {
                        val allowed = filters.value.type != null
                        error.value = !allowed
                        allowed
                    },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.category)).performClick()
        compose.onNodeWithText(context.getString(R.string.error_select_type_first)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.category_food)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.type)).performClick()
        compose.onNodeWithText(context.getString(R.string.expense)).performClick()
        compose.onNodeWithText(context.getString(R.string.error_select_type_first)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.category)).performClick()
        compose.onNodeWithText(context.getString(R.string.category_salary)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.category_food)).performClick()
        compose.onNodeWithText(context.getString(R.string.date)).performClick()
        compose.onNodeWithText(context.getString(R.string.today)).performClick()
        compose.runOnIdle {
            assertEquals(HistoryFilters(DateFilter.TODAY, TransactionType.EXPENSE, CategoryType.FOOD), filters.value)
            assertFalse(error.value)
        }
        compose.onNodeWithText(context.getString(R.string.expense)).performClick()
        compose.onNodeWithText(context.getString(R.string.income)).performClick()
        compose.onNodeWithText(context.getString(R.string.category)).performClick()
        compose.onNodeWithText(context.getString(R.string.category_food)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.category_salary)).performClick()
        compose.runOnIdle {
            assertEquals(HistoryFilters(DateFilter.TODAY, TransactionType.INCOME, CategoryType.SALARY), filters.value)
        }
    }
}

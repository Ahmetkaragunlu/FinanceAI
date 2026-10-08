package com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper

import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetWarning
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Rule
import org.junit.Test

class BudgetWarningTextTest {
    @get:Rule val compose = createComposeRule()

    @Test fun typedWarningsKeepEveryExistingXmlMessageAndItsArguments() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val food = context.getString(R.string.category_food)
        val cases = listOf(
            BudgetWarning.GeneralExceeded to context.getString(R.string.warning_budget_exceeded),
            BudgetWarning.GeneralNearLimit to context.getString(R.string.warning_budget_near_end),
            BudgetWarning.CategoryExceeded(CategoryType.FOOD) to context.getString(R.string.warning_category_exceeded, food),
            BudgetWarning.CategoriesExceeded(2) to context.getString(R.string.warning_multiple_categories_exceeded, 2),
            BudgetWarning.GeneralAndCategoryExceeded(CategoryType.FOOD) to context.getString(R.string.warning_budget_and_category_exceeded, food),
            BudgetWarning.GeneralAndCategoriesExceeded(2) to context.getString(R.string.warning_budget_and_multiple_categories, 2),
        )
        val warning = mutableStateOf<BudgetWarning>(cases.first().first)
        compose.setContent { Text(warning.value.localizedText()) }
        for ((value, expected) in cases) {
            compose.runOnIdle { warning.value = value }
            compose.onNodeWithText(expected).assertIsDisplayed()
        }
    }
}

package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BudgetCategorySelectorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dropdownSelectionAndControlledErrorKeepTheirExistingBehaviour() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val category = mutableStateOf<CategoryType?>(null)
        val error = mutableStateOf<Int?>(R.string.error_select_category)
        compose.setContent {
            MaterialTheme {
                BudgetCategorySelector(category.value, listOf(CategoryType.FOOD, CategoryType.TRANSPORT),
                    { category.value = it; error.value = null }, error.value)
            }
        }
        compose.onNodeWithText(context.getString(R.string.choose_placeholder)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.error_select_category)).assertIsDisplayed()
        compose.onAllNodes(hasClickAction())[0].performClick()
        compose.onNodeWithText(context.getString(R.string.category_food)).performClick()
        compose.runOnIdle { assertEquals(CategoryType.FOOD, category.value) }
        compose.onNodeWithText(context.getString(R.string.category_food)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.error_select_category)).assertDoesNotExist()
    }
}

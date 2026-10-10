package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BudgetTypeSelectorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun percentageAvailabilityAndTypeCallbacksRemainControlledByTheParent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val enabled = mutableStateOf(false)
        val selected = mutableListOf<BudgetType>()
        compose.setContent {
            MaterialTheme {
                BudgetTypeSelector(BudgetType.CATEGORY_AMOUNT, { selected += it },
                    FinanceColors.sheetSurface, FinanceColors.elevatedSurface, enabled.value)
            }
        }
        compose.onNodeWithText(context.getString(R.string.budget_type_percentage)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.budget_type_category)).performClick()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithText(context.getString(R.string.budget_type_percentage)).performClick()
        compose.runOnIdle {
            assertEquals(listOf(BudgetType.CATEGORY_AMOUNT, BudgetType.CATEGORY_PERCENTAGE), selected)
        }
    }
}

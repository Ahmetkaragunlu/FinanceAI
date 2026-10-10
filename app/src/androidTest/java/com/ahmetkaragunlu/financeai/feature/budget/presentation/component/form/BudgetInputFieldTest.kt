package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BudgetInputFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun inputSuffixAndControlledErrorKeepTheExistingFieldContract() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val value = mutableStateOf("")
        compose.setContent {
            MaterialTheme {
                BudgetInputField(value = value.value, onValueChange = { value.value = it },
                    label = context.getString(R.string.amount_label), suffix = "USD",
                    borderColor = FinanceColors.summaryEnd, errorResId = R.string.error_invalid_amount)
            }
        }
        compose.onNodeWithText("USD").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.error_invalid_amount)).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("25.5")
        compose.runOnIdle { assertEquals("25.5", value.value) }
    }
}

package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TransactionDatePickerDialogTest {
    @get:Rule val compose = createComposeRule()
    private val date = Instant.parse("2026-10-10T00:00:00Z").toEpochMilli()

    @Test
    fun confirmationPassesThePickerTimestampToTheOwner() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var selected: Long? = null
        compose.setContent {
            MaterialTheme {
                TransactionDatePickerDialog(date, { true }, { selected = it }, {})
            }
        }
        compose.onNodeWithText(context.getString(R.string.ok)).performClick()
        compose.runOnIdle { assertEquals(date, selected) }
    }

    @Test
    fun changedValidationCannotConfirmThePreviouslySelectedDate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val valid = mutableStateOf(true)
        var selected: Long? = null
        compose.setContent {
            MaterialTheme {
                TransactionDatePickerDialog(date, { valid.value }, { selected = it }, {})
            }
        }
        compose.runOnIdle { valid.value = false }
        compose.onNodeWithText(context.getString(R.string.ok)).performClick()
        compose.runOnIdle { assertNull(selected) }
    }

    @Test
    fun cancellationDismissesWithoutSelectingADate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var selected: Long? = null
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                TransactionDatePickerDialog(date, { true }, { selected = it }, { dismissals++ })
            }
        }
        compose.onNodeWithText(context.getString(R.string.cancel)).performClick()
        compose.runOnIdle {
            assertEquals(1, dismissals)
            assertNull(selected)
        }
    }
}

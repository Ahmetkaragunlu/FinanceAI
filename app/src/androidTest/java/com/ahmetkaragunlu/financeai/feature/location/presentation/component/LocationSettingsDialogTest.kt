package com.ahmetkaragunlu.financeai.feature.location.presentation.component

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationSettingsDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun confirmationOpensSettingsBeforeDismissalAndCancelOnlyDismisses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val visible = mutableStateOf(true)
        val events = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                if (visible.value) {
                    LocationSettingsDialog(onOpenSettings = { events += "open" },
                        onDismiss = { events += "dismiss"; visible.value = false })
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.open_settings)).performClick()
        compose.runOnIdle {
            assertEquals(listOf("open", "dismiss"), events)
            visible.value = true
        }
        compose.onNodeWithText(context.getString(R.string.cancel)).performClick()
        compose.runOnIdle { assertEquals(listOf("open", "dismiss", "dismiss"), events) }
    }
}

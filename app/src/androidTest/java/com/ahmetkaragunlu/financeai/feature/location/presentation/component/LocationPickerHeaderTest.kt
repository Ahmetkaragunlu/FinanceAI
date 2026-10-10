package com.ahmetkaragunlu.financeai.feature.location.presentation.component

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.presentation.LocationPickerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationPickerHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun searchClearCloseAndCurrentLocationKeepTheirCallbacksAndLoadingGate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = mutableStateOf(LocationPickerUiState())
        var searches = 0
        var currentRequests = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                LocationPickerHeader(state.value, { currentRequests++ },
                    { state.value = state.value.copy(searchQuery = it) },
                    { searches++ }, { dismissals++ })
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Synthetic address")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithContentDescription(context.getString(R.string.clear_search)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.current_location)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.close)).performClick()
        compose.runOnIdle {
            assertEquals("", state.value.searchQuery)
            assertEquals(1, searches)
            assertEquals(1, currentRequests)
            assertEquals(1, dismissals)
            state.value = state.value.copy(isLoading = true)
        }
        compose.onNodeWithContentDescription(context.getString(R.string.current_location)).assertIsNotEnabled()
    }
}

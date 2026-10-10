package com.ahmetkaragunlu.financeai.feature.location.presentation.component

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.presentation.LocationPickerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationPickerOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun confirmationSelectsBeforeDismissalAndFallbackWithoutSelectionDoesNotDispatch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val position = Coordinates(41.0, 29.0)
        val state = mutableStateOf(LocationPickerUiState(
            selectedLocation = position, addressText = "Synthetic address"
        ))
        val events = mutableListOf<String>()
        val selections = mutableListOf<Coordinates>()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    LocationPickerOverlay(state.value,
                        { latitude, longitude -> selections += Coordinates(latitude, longitude); events += "select" },
                        { events += "dismiss" })
                }
            }
        }
        compose.onNodeWithText("Synthetic address").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.confirm_location)).performClick()
        compose.runOnIdle {
            assertEquals(listOf(position), selections)
            assertEquals(listOf("select", "dismiss"), events)
            events.clear()
            state.value = LocationPickerUiState(fallbackCoordinates = position)
        }
        compose.onNodeWithText(context.getString(R.string.location_coordinates,
            position.latitude, position.longitude)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.confirm_location)).performClick()
        compose.runOnIdle {
            assertEquals(emptyList<String>(), events)
            assertEquals(listOf(position), selections)
        }
    }
}

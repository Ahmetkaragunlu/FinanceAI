package com.ahmetkaragunlu.financeai.feature.location.presentation

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationPresentationTest {
    @get:Rule
    val compose = createComposeRule()
    @Test
    fun sdkNormalizationKeepsExistingClampWrapAndBoundaryBehaviour() {
        listOf(
            Coordinates(41.0, 29.0), Coordinates(100.0, 540.0), Coordinates(-100.0, -540.0),
            Coordinates(0.0, 180.0), Coordinates(-0.0, -180.0), Coordinates(90.0, 179.999),
            Coordinates(Double.NaN, Double.POSITIVE_INFINITY)
        ).forEach {
            val expected = LatLng(it.latitude, it.longitude)
            assertEquals(Coordinates(expected.latitude, expected.longitude), it.normalizedForMap())
        }
    }

    @Test
    fun coordinateFallbackUsesTheSameXmlAndAnAddressFailureKeepsItsExistingMessage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = LocationPickerUiState(fallbackCoordinates = Coordinates(41.123, 29.456))
        compose.setContent { Text(state.addressDisplayText().orEmpty()) }
        compose.onNodeWithText(context.getString(R.string.location_coordinates, 41.123, 29.456))
            .assertIsDisplayed()
        assertEquals(R.string.address_not_found, LocationFailure.AddressNotFound.messageRes())
    }
}

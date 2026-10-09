package com.ahmetkaragunlu.financeai.feature.location.presentation

import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure

data class LocationPickerUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val currentLocation: Coordinates? = null,
    val selectedLocation: Coordinates? = null,
    val addressText: String? = null,
    val fallbackCoordinates: Coordinates? = null,
    val isLoading: Boolean = false,
    val error: LocationFailure? = null,
    val hasLocationPermission: Boolean = false,
)

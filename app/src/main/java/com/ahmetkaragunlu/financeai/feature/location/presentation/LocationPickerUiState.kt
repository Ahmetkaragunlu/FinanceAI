package com.ahmetkaragunlu.financeai.feature.location.presentation

import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure
import com.google.android.gms.maps.model.LatLng

data class LocationPickerUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val currentLocation: LatLng? = null,
    val selectedLocation: LatLng? = null,
    val addressText: String? = null,
    val isLoading: Boolean = false,
    val error: LocationFailure? = null,
    val hasLocationPermission: Boolean = false,
)

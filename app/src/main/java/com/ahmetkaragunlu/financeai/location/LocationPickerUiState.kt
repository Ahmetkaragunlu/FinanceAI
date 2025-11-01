package com.ahmetkaragunlu.financeai.location

import com.google.android.gms.maps.model.LatLng

data class LocationPickerUiState(
    val currentLocation: LatLng? = null,
    val selectedLocation: LatLng? = null,
    val addressText: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasLocationPermission: Boolean = false,
    val showLocationSettingsDialog: Boolean = false
)
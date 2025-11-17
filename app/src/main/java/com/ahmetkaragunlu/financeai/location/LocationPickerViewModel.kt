package com.ahmetkaragunlu.financeai.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationPickerUiState())
    val uiState: StateFlow<LocationPickerUiState> = _uiState.asStateFlow()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    init {
        checkLocationPermission()
    }
    fun updatePermissionState(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationPermission = isGranted)
        if (!isGranted) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.location_permission_required)
            )
        }
    }
    private fun checkLocationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        if (!_uiState.value.hasLocationPermission) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.location_permission_required)
            )
            return
        }

        if (!isLocationEnabled()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.gps_disabled),
                showLocationSettingsDialog = true
            )
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        selectLocation(latLng)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = context.getString(R.string.location_not_available)
                        )
                    }
                }.addOnFailureListener { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.location_error, exception.localizedMessage ?: "")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.location_not_available)
                )
            }
        }
    }

    fun selectLocation(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = latLng,
            currentLocation = latLng,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                val locationData = LocationUtil.getAddressFromLocation(
                    context,
                    latLng.latitude,
                    latLng.longitude
                )

                _uiState.value = _uiState.value.copy(
                    addressText = locationData?.addressFull ?: context.getString(R.string.address_not_found),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    addressText = context.getString(R.string.location_coordinates, latLng.latitude, latLng.longitude),
                    isLoading = false
                )
            }
        }
    }
    fun dismissLocationSettingsDialog() {
        _uiState.value = _uiState.value.copy(showLocationSettingsDialog = false)
    }
}
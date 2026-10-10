package com.ahmetkaragunlu.financeai.feature.location.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.ahmetkaragunlu.financeai.feature.location.presentation.LocationPickerUiState

@Composable
internal fun LocationPickerMap(
    uiState: LocationPickerUiState,
    onPositionSelected: (Coordinates) -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position =
            CameraPosition.fromLatLngZoom(
                uiState.currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(41.0082, 28.9784),
                15f,
            )
    }

    LaunchedEffect(uiState.selectedLocation) {
        uiState.selectedLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f)
            )
        }
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = uiState.hasLocationPermission),
        uiSettings =
            MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false),
        onMapClick = { latLng -> onPositionSelected(Coordinates(latLng.latitude, latLng.longitude)) },
    ) {
        uiState.selectedLocation?.let { location ->
            val mapPosition = LatLng(location.latitude, location.longitude)
            val markerState = rememberMarkerState(position = mapPosition)

            LaunchedEffect(location) {
                if (
                    markerState.dragState == DragState.END &&
                        markerState.position != mapPosition
                )
                    markerState.position = mapPosition
            }

            LaunchedEffect(markerState.dragState) {
                if (
                    markerState.dragState == DragState.END &&
                        markerState.position != mapPosition
                ) {
                    onPositionSelected(Coordinates(markerState.position.latitude, markerState.position.longitude))
                }
            }

            Marker(
                state = markerState,
                title = stringResource(R.string.select_location),
                draggable = true,
            )
        }
    }
}

package com.ahmetkaragunlu.financeai.feature.location.presentation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.presentation.component.LocationPickerHeader
import com.ahmetkaragunlu.financeai.feature.location.presentation.component.LocationPickerMap
import com.ahmetkaragunlu.financeai.feature.location.presentation.component.LocationPickerOverlay
import com.ahmetkaragunlu.financeai.feature.location.presentation.component.LocationSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerRoute(
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    onDismiss: () -> Unit,
    viewModel: LocationPickerViewModel = hiltViewModel(),
) {
    BackHandler { onDismiss() }
    var showLocationSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val requestCurrentLocation: () -> Unit = {
        viewModel.getCurrentLocation(onSettingsRequired = { showLocationSettingsDialog = true })
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestSelection by rememberUpdatedState(uiState.selectedLocation)
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            val hasPermission = fineLocationGranted || coarseLocationGranted
            viewModel.updatePermissionState(hasPermission)
            if (hasPermission) {
                requestCurrentLocation()
            }
        }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = viewModel.refreshPermission()
                if (hasPermission && viewModel.isLocationEnabled() && latestSelection == null) {
                    requestCurrentLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        val hasPermission = viewModel.refreshPermission()
        if (hasPermission) {
            requestCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    ToastMessageEffect(uiState.error?.messageRes())

    MapLocationPickerScreen(
        uiState = uiState,
        showLocationSettingsDialog = showLocationSettingsDialog,
        onSettingsDismissed = { showLocationSettingsDialog = false },
        requestCurrentLocation = requestCurrentLocation,
        onQueryChanged = viewModel::updateSearchQuery,
        onSearch = viewModel::search,
        onPositionSelected = viewModel::selectLocation,
        onLocationSelected = onLocationSelected,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerScreen(
    uiState: LocationPickerUiState,
    showLocationSettingsDialog: Boolean,
    onSettingsDismissed: () -> Unit,
    requestCurrentLocation: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onPositionSelected: (Coordinates) -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            LocationPickerHeader(
                uiState = uiState,
                requestCurrentLocation = requestCurrentLocation,
                onQueryChanged = onQueryChanged,
                onSearch = onSearch,
                onDismiss = onDismiss,
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LocationPickerMap(uiState = uiState, onPositionSelected = onPositionSelected)
            LocationPickerOverlay(
                uiState = uiState,
                onLocationSelected = onLocationSelected,
                onDismiss = onDismiss,
            )
        }
    }
    if (showLocationSettingsDialog) {
        LocationSettingsDialog(
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            },
            onDismiss = onSettingsDismissed,
        )
    }
}

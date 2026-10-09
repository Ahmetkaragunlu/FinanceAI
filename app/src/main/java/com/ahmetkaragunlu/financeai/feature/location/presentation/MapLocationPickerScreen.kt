package com.ahmetkaragunlu.financeai.feature.location.presentation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.select_location),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.close))
                        }
                    },
                    actions = {
                        IconButton(onClick = requestCurrentLocation, enabled = !uiState.isLoading) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.current_location),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colorResource(R.color.background),
                            titleContentColor = FinanceColors.onAccent,
                            navigationIconContentColor = FinanceColors.onAccent,
                        ),
                )
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_address_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.clear_search))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FinanceColors.onAccent,
                            unfocusedContainerColor = FinanceColors.onAccent,
                        ),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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

            if (uiState.isLoading || uiState.isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            val addressText = uiState.addressDisplayText()
            addressText?.let { address ->
                Card(
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(Spacing.screenPadding),
                    colors = CardDefaults.cardColors(containerColor = FinanceColors.dialogSurface),
                ) {
                    Text(
                        text = address,
                        modifier = Modifier.padding(Spacing.screenPadding),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinanceColors.onAccent,
                    )
                }
                Button(
                    onClick = {
                        uiState.selectedLocation?.let { location ->
                            onLocationSelected(location.latitude, location.longitude)
                            onDismiss()
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.background)
                        ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.confirm_location))
                }
            }
        }
    }
    if (showLocationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { onSettingsDismissed() },
            icon = { Icon(Icons.Default.LocationOff, null) },
            title = { Text(stringResource(R.string.location_services_disabled)) },
            text = { Text(stringResource(R.string.location_services_disabled_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        onSettingsDismissed()
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { onSettingsDismissed() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

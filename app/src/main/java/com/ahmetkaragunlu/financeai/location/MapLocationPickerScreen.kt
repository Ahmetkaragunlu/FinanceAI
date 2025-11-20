package com.ahmetkaragunlu.financeai.location

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerScreen(
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    onDismiss: () -> Unit ,
    viewModel: LocationPickerViewModel = hiltViewModel(),


) {
    BackHandler {onDismiss()}
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val hasPermission = fineLocationGranted || coarseLocationGranted
        viewModel.updatePermissionState(hasPermission)
        if (hasPermission) {
            viewModel.getCurrentLocation()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission && viewModel.isLocationEnabled()) {
                    viewModel.getCurrentLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.select_location), style = MaterialTheme.typography.bodyLarge) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.getCurrentLocation() },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(R.color.background),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White

                    )
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder ={ Text(stringResource(R.string.search_address_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            scope.launch {
                                isSearching = true
                                LocationUtil.searchLocation(context, searchQuery)?.let { latLng ->
                                    viewModel.selectLocation(latLng)
                                }
                                isSearching = false
                            }
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,

                        ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },

        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    uiState.currentLocation ?: LatLng(41.0082, 28.9784),
                    15f
                )
            }

            LaunchedEffect(uiState.selectedLocation) {
                uiState.selectedLocation?.let { location ->
                    cameraPositionState.animate(
                        update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                            location,
                            16f
                        )
                    )
                }
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = uiState.hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                ),
                onMapClick = { latLng ->
                    viewModel.selectLocation(latLng)
                }
            ) {
                uiState.selectedLocation?.let { location ->
                    val markerState = rememberMarkerState(position = location)

                    LaunchedEffect(markerState.position) {
                        if (markerState.position != location) {
                            viewModel.selectLocation(markerState.position)
                        }
                    }

                    Marker(
                        state = markerState,
                        title = stringResource(R.string.select_location),
                        draggable = true
                    )
                }
            }

            if (uiState.isLoading || isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.addressText?.let { address ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2B2D31)
                    )
                ) {
                    Text(
                        text = address,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                Button(
                    onClick = {
                        uiState.selectedLocation?.let { location ->
                            onLocationSelected(location.latitude, location.longitude)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.background)),
                    modifier = Modifier.align(Alignment.BottomCenter)

                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.confirm_location))
                }
            }
        }
    }
    if (uiState.showLocationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLocationSettingsDialog() },
            icon = { Icon(Icons.Default.LocationOff, null) },
            title = { Text(stringResource(R.string.location_services_disabled)) },
            text = { Text(stringResource(R.string.location_services_disabled_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.openLocationSettings()
                    viewModel.dismissLocationSettingsDialog()
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLocationSettingsDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
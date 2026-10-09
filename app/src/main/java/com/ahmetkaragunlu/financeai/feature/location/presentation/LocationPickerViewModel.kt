package com.ahmetkaragunlu.financeai.feature.location.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.LocationGateway
import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LocationPickerViewModel
@Inject
constructor(
    private val locations: LocationGateway,
    private val addresses: AddressResolver,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LocationPickerUiState())
    val uiState = mutableState.asStateFlow()
    private var requestId = 0L
    private var requestJob: Job? = null

    init {
        refreshPermission()
    }

    fun updateSearchQuery(query: String) {
        mutableState.value = mutableState.value.copy(searchQuery = query)
    }

    fun refreshPermission(): Boolean {
        val granted = locations.hasPermission()
        updatePermissionState(granted)
        return granted
    }

    fun updatePermissionState(isGranted: Boolean) {
        mutableState.value =
            mutableState.value.copy(
                hasLocationPermission = isGranted,
                error = if (isGranted) null else LocationFailure.PermissionDenied,
            )
        if (!isGranted) {
            requestId++
            requestJob?.cancel()
            mutableState.value = mutableState.value.copy(isLoading = false, isSearching = false)
        }
    }

    fun isLocationEnabled() = locations.isEnabled()

    private fun begin(): Long {
        requestJob?.cancel()
        mutableState.value = mutableState.value.copy(isSearching = false)
        return ++requestId
    }

    fun getCurrentLocation(onSettingsRequired: () -> Unit = {}) {
        if (!refreshPermission()) return
        if (!locations.isEnabled()) {
            mutableState.value = mutableState.value.copy(error = LocationFailure.ServicesDisabled)
            onSettingsRequired()
            return
        }
        val id = begin()
        mutableState.value = mutableState.value.copy(isLoading = true, error = null)
        requestJob =
            viewModelScope.launch {
                try {
                    val coordinate = locations.current()
                    if (id != requestId) return@launch
                    if (coordinate == null) fail(id, LocationFailure.PositionUnavailable)
                    else resolve(coordinate, id)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    fail(id, LocationFailure.PositionUnavailable)
                }
            }
    }

    fun selectLocation(value: Coordinates) {
        val id = begin()
        requestJob =
            viewModelScope.launch { resolve(value, id) }
    }

    fun search() {
        val query = mutableState.value.searchQuery.trim()
        if (query.isEmpty()) return
        val id = begin()
        mutableState.value = mutableState.value.copy(isSearching = true)
        requestJob =
            viewModelScope.launch {
                try {
                    val result = addresses.search(query)
                    if (id != requestId) return@launch
                    if (result == null) fail(id, LocationFailure.AddressNotFound)
                    else resolve(result, id)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    fail(id, LocationFailure.AddressNotFound)
                } finally {
                    if (id == requestId)
                        mutableState.value = mutableState.value.copy(isSearching = false)
                }
            }
    }

    private suspend fun resolve(coordinate: Coordinates, id: Long) {
        if (id != requestId) return
        val selected = coordinate.normalizedForMap()
        mutableState.value =
            mutableState.value.copy(
                selectedLocation = selected,
                currentLocation = selected,
                isLoading = true,
                error = null,
            )
        try {
            val address = addresses.resolve(coordinate)
            if (id == requestId)
                mutableState.value =
                    mutableState.value.copy(
                        addressText = address?.addressFull,
                        fallbackCoordinates = coordinate.takeIf { address == null },
                        error = if (address == null) LocationFailure.AddressNotFound else null,
                        isLoading = false,
                    )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (id == requestId)
                mutableState.value =
                    mutableState.value.copy(
                        addressText = null,
                        fallbackCoordinates = coordinate,
                        error = LocationFailure.AddressNotFound,
                        isLoading = false,
                    )
        }
    }

    private fun fail(id: Long, failure: LocationFailure) {
        if (id == requestId)
            mutableState.value = mutableState.value.copy(isLoading = false, error = failure)
    }
}

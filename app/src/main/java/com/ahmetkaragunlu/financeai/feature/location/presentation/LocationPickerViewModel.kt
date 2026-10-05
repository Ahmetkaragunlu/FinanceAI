package com.ahmetkaragunlu.financeai.feature.location.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.LocationGateway
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locations: LocationGateway,
    private val addresses: AddressResolver
) : ViewModel() {
    private val mutableState = MutableStateFlow(LocationPickerUiState())
    val uiState = mutableState.asStateFlow()
    private var requestId = 0L
    private var requestJob: Job? = null

    init { refreshPermission() }

    fun updateSearchQuery(query: String) {
        mutableState.value = mutableState.value.copy(searchQuery = query)
    }
    fun refreshPermission(): Boolean {
        val granted = locations.hasPermission()
        updatePermissionState(granted)
        return granted
    }
    fun updatePermissionState(isGranted: Boolean) {
        mutableState.value = mutableState.value.copy(hasLocationPermission = isGranted,
            errorMessage = if (isGranted) null else context.getString(R.string.location_permission_required))
        if (!isGranted) { requestId++; requestJob?.cancel(); mutableState.value = mutableState.value.copy(isLoading = false, isSearching = false) }
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
            mutableState.value = mutableState.value.copy(errorMessage = context.getString(R.string.gps_disabled))
            onSettingsRequired()
            return
        }
        val id = begin()
        mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
        requestJob = viewModelScope.launch {
            try {
                val coordinate = locations.current()
                if (id != requestId) return@launch
                if (coordinate == null) fail(id, R.string.location_not_available) else resolve(coordinate, id)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail(id, R.string.location_not_available) }
        }
    }
    fun selectLocation(value: LatLng) {
        val id = begin()
        requestJob = viewModelScope.launch { resolve(Coordinates(value.latitude, value.longitude), id) }
    }
    fun search() {
        val query = mutableState.value.searchQuery.trim()
        if (query.isEmpty()) return
        val id = begin()
        mutableState.value = mutableState.value.copy(isSearching = true)
        requestJob = viewModelScope.launch {
            try {
                val result = addresses.search(query)
                if (id != requestId) return@launch
                if (result == null) fail(id, R.string.address_not_found) else resolve(result, id)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail(id, R.string.address_not_found) }
            finally { if (id == requestId) mutableState.value = mutableState.value.copy(isSearching = false) }
        }
    }
    private suspend fun resolve(coordinate: Coordinates, id: Long) {
        if (id != requestId) return
        val selected = LatLng(coordinate.latitude, coordinate.longitude)
        mutableState.value = mutableState.value.copy(selectedLocation = selected, currentLocation = selected,
            isLoading = true, errorMessage = null)
        try {
            val address = addresses.resolve(coordinate)
            if (id == requestId) mutableState.value = mutableState.value.copy(addressText = address?.addressFull
                ?: context.getString(R.string.location_coordinates, coordinate.latitude, coordinate.longitude),
                errorMessage = if (address == null) context.getString(R.string.address_not_found) else null, isLoading = false)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (id == requestId) mutableState.value = mutableState.value.copy(
                addressText = context.getString(R.string.location_coordinates, coordinate.latitude, coordinate.longitude),
                errorMessage = context.getString(R.string.address_not_found), isLoading = false)
        }
    }
    private fun fail(id: Long, message: Int) {
        if (id == requestId) mutableState.value = mutableState.value.copy(isLoading = false, errorMessage = context.getString(message))
    }
}

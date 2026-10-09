package com.ahmetkaragunlu.financeai.feature.location.presentation

import androidx.lifecycle.ViewModelStore
import com.ahmetkaragunlu.financeai.core.coroutines.testing.MainDispatcherRule
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.LocationGateway
import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPickerViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val holder = ViewModelStore()
    private class Gateway : LocationGateway {
        var granted = true
        var enabled = true
        var onCurrent: suspend () -> Coordinates? = { null }
        override fun hasPermission() = granted
        override fun isEnabled() = enabled
        override suspend fun current() = onCurrent()
    }
    private class Addresses : AddressResolver {
        var onSearch: suspend (String) -> Coordinates? = { null }
        var onResolve: suspend (Coordinates) -> LocationData? = { data(it, "address") }
        override suspend fun search(query: String) = onSearch(query)
        override suspend fun resolve(coordinates: Coordinates) = onResolve(coordinates)
    }
    private fun model(gateway: Gateway = Gateway(), addresses: Addresses = Addresses()) =
        LocationPickerViewModel(gateway, addresses).also { holder.put("location", it) }
    @After fun close() { holder.clear() }

    @Test fun lateAddressCannotOverwriteTheLatestManualSelection() = runTest {
        val release = CompletableDeferred<Unit>()
        val addresses = Addresses().apply {
            onResolve = {
                if (it.latitude == 1.0) withContext(NonCancellable) { release.await() }
                data(it, "${it.latitude}")
            }
        }
        val vm = model(addresses = addresses)
        try {
            vm.selectLocation(Coordinates(1.0, 1.0)); runCurrent()
            vm.selectLocation(Coordinates(2.0, 2.0)); runCurrent()
            release.complete(Unit); runCurrent()
            assertEquals(Coordinates(2.0, 2.0), vm.uiState.value.selectedLocation)
            assertEquals("2.0", vm.uiState.value.addressText)
        } finally { release.complete(Unit) }
    }

    @Test fun anEmptySearchResultEndsLoadingAndProducesAnExistingXmlError() = runTest {
        val vm = model()
        vm.updateSearchQuery("unknown"); vm.search(); runCurrent()
        assertEquals(LocationFailure.AddressNotFound, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSearching)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun cancelledSearchCannotClearTheLatestSearchStatusOrReplaceItsResult() = runTest {
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        val addresses = Addresses().apply {
            onSearch = {
                if (it == "first") {
                    withContext(NonCancellable) { first.await() }
                    Coordinates(1.0, 1.0)
                } else {
                    second.await()
                    Coordinates(2.0, 2.0)
                }
            }
            onResolve = { data(it, "latest") }
        }
        val vm = model(addresses = addresses)
        try {
            vm.updateSearchQuery("first"); vm.search(); runCurrent()
            vm.updateSearchQuery("second"); vm.search(); runCurrent()
            first.complete(Unit); runCurrent()
            assertTrue(vm.uiState.value.isSearching)
            assertNull(vm.uiState.value.selectedLocation)
            second.complete(Unit); runCurrent()
            assertEquals(Coordinates(2.0, 2.0), vm.uiState.value.selectedLocation)
            assertEquals("latest", vm.uiState.value.addressText)
            assertFalse(vm.uiState.value.isSearching)
        } finally { first.complete(Unit); second.complete(Unit) }
    }

    @Test fun deniedPermissionDisabledGpsAndMissingPositionKeepTheirDistinctResults() = runTest {
        val gateway = Gateway().apply { granted = false }
        val vm = model(gateway)
        var settings = 0
        vm.getCurrentLocation { settings++ }; runCurrent()
        assertEquals(LocationFailure.PermissionDenied, vm.uiState.value.error)
        gateway.granted = true; gateway.enabled = false
        vm.getCurrentLocation { settings++ }; runCurrent()
        assertEquals(LocationFailure.ServicesDisabled, vm.uiState.value.error)
        assertEquals(1, settings)
        gateway.enabled = true
        vm.getCurrentLocation(); runCurrent()
        assertEquals(LocationFailure.PositionUnavailable, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        gateway.onCurrent = { throw IllegalStateException("private") }
        vm.getCurrentLocation(); runCurrent()
        assertEquals(LocationFailure.PositionUnavailable, vm.uiState.value.error)
        gateway.onCurrent = { throw CancellationException() }
        vm.getCurrentLocation(); runCurrent()
        assertNull(vm.uiState.value.error)
    }

    @Test fun permissionRevocationInvalidatesAPendingPositionResult() = runTest {
        val release = CompletableDeferred<Unit>()
        val gateway = Gateway().apply {
            onCurrent = { withContext(NonCancellable) { release.await() }; Coordinates(3.0, 4.0) }
        }
        val vm = model(gateway)
        try {
            vm.getCurrentLocation(); runCurrent()
            gateway.granted = false; vm.refreshPermission()
            release.complete(Unit); runCurrent()
            assertNull(vm.uiState.value.selectedLocation)
            assertEquals(LocationFailure.PermissionDenied, vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
        } finally { release.complete(Unit) }
    }

    @Test fun missingAddressKeepsOriginalFallbackCoordinatesAndNormalizedSelection() = runTest {
        val addresses = Addresses().apply { onResolve = { null } }
        val vm = model(addresses = addresses)
        val raw = Coordinates(100.0, 540.0)
        vm.selectLocation(raw); runCurrent()
        assertEquals(Coordinates(90.0, -180.0), vm.uiState.value.selectedLocation)
        assertEquals(raw, vm.uiState.value.fallbackCoordinates)
        assertNull(vm.uiState.value.addressText)
        assertEquals(LocationFailure.AddressNotFound, vm.uiState.value.error)
        addresses.onResolve = { data(it, "resolved") }
        vm.selectLocation(Coordinates(2.0, 3.0)); runCurrent()
        assertEquals("resolved", vm.uiState.value.addressText)
        assertNull(vm.uiState.value.fallbackCoordinates)
        addresses.onResolve = { throw IllegalStateException("private") }
        vm.selectLocation(Coordinates(4.0, 5.0)); runCurrent()
        assertEquals(Coordinates(4.0, 5.0), vm.uiState.value.fallbackCoordinates)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun blankSearchDoesNothingAndNonblankSearchUsesTheTrimmedQuery() = runTest {
        val queries = mutableListOf<String>()
        val addresses = Addresses().apply { onSearch = { queries += it; Coordinates(1.0, 2.0) } }
        val vm = model(addresses = addresses)
        vm.updateSearchQuery("   "); vm.search(); runCurrent()
        assertTrue(queries.isEmpty())
        vm.updateSearchQuery("  address  "); vm.search(); runCurrent()
        assertEquals(listOf("address"), queries)
        assertEquals(Coordinates(1.0, 2.0), vm.uiState.value.selectedLocation)
        assertFalse(vm.uiState.value.isSearching)
    }

    companion object {
        private fun data(value: Coordinates, text: String) =
            LocationData(value.latitude, value.longitude, text, "address")
    }
}

package com.ahmetkaragunlu.financeai.feature.location.presentation

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.LocationGateway
import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPickerViewModelTest {
    private val gateway =
        object : LocationGateway {
            override fun hasPermission() = true

            override fun isEnabled() = true

            override suspend fun current(): Coordinates? = null
        }

    @Test
    fun lateAddressCannotOverwriteTheLatestManualSelection() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val resolver =
            object : AddressResolver {
                override suspend fun search(query: String): Coordinates? = null

                override suspend fun resolve(coordinates: Coordinates): LocationData {
                    if (coordinates.latitude == 1.0)
                        withContext(NonCancellable) {
                            entered.complete(Unit)
                            release.await()
                        }
                    return LocationData(
                        coordinates.latitude,
                        coordinates.longitude,
                        "${coordinates.latitude}",
                        "address",
                    )
                }
            }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel =
            withContext(Dispatchers.Main) { LocationPickerViewModel(context, gateway, resolver) }
        val holder = ViewModelStore().apply { put("location", viewModel) }
        try {
            withContext(Dispatchers.Main) { viewModel.selectLocation(LatLng(1.0, 1.0)) }
            withTimeout(5_000) { entered.await() }
            withContext(Dispatchers.Main) { viewModel.selectLocation(LatLng(2.0, 2.0)) }
            withTimeout(5_000) { viewModel.uiState.first { it.addressText == "2.0" } }
            release.complete(Unit)
            withContext(Dispatchers.Main) { yield() }
            assertEquals(LatLng(2.0, 2.0), viewModel.uiState.value.selectedLocation)
            assertEquals("2.0", viewModel.uiState.value.addressText)
        } finally {
            release.complete(Unit)
            withContext(Dispatchers.Main) { holder.clear() }
        }
    }

    @Test
    fun anEmptySearchResultEndsLoadingAndProducesAnExistingXmlError() = runBlocking {
        val resolver =
            object : AddressResolver {
                override suspend fun search(query: String): Coordinates? = null

                override suspend fun resolve(coordinates: Coordinates): LocationData? = null
            }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel =
            withContext(Dispatchers.Main) { LocationPickerViewModel(context, gateway, resolver) }
        val holder = ViewModelStore().apply { put("location", viewModel) }
        try {
            withContext(Dispatchers.Main) {
                viewModel.updateSearchQuery("unknown")
                viewModel.search()
            }
            withTimeout(5_000) { viewModel.uiState.first { it.error != null } }
            assertEquals(LocationFailure.AddressNotFound, viewModel.uiState.value.error)
            assertEquals(R.string.address_not_found, checkNotNull(viewModel.uiState.value.error).messageRes())
            assertFalse(viewModel.uiState.value.isSearching)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            withContext(Dispatchers.Main) { holder.clear() }
        }
    }

    @Test
    fun cancelledSearchCannotClearTheLatestSearchStatusOrReplaceItsResult() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()
        val resolver =
            object : AddressResolver {
                override suspend fun search(query: String): Coordinates {
                    if (query == "first") {
                        withContext(NonCancellable) {
                            firstEntered.complete(Unit)
                            releaseFirst.await()
                        }
                        return Coordinates(1.0, 1.0)
                    }
                    secondEntered.complete(Unit)
                    releaseSecond.await()
                    return Coordinates(2.0, 2.0)
                }

                override suspend fun resolve(coordinates: Coordinates) =
                    LocationData(coordinates.latitude, coordinates.longitude, "latest", "address")
            }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel =
            withContext(Dispatchers.Main) { LocationPickerViewModel(context, gateway, resolver) }
        val holder = ViewModelStore().apply { put("location", viewModel) }
        try {
            withContext(Dispatchers.Main) {
                viewModel.updateSearchQuery("first")
                viewModel.search()
            }
            withTimeout(5_000) { firstEntered.await() }
            withContext(Dispatchers.Main) {
                viewModel.updateSearchQuery("second")
                viewModel.search()
            }
            withTimeout(5_000) { secondEntered.await() }
            releaseFirst.complete(Unit)
            withContext(Dispatchers.Main) { yield() }
            assertEquals("second", viewModel.uiState.value.searchQuery)
            assertTrue(viewModel.uiState.value.isSearching)
            assertNull(viewModel.uiState.value.selectedLocation)

            releaseSecond.complete(Unit)
            withTimeout(5_000) {
                viewModel.uiState.first {
                    it.selectedLocation == LatLng(2.0, 2.0) && !it.isSearching
                }
            }
            assertEquals("latest", viewModel.uiState.value.addressText)
        } finally {
            releaseFirst.complete(Unit)
            releaseSecond.complete(Unit)
            withContext(Dispatchers.Main) { holder.clear() }
        }
    }
}

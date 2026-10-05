package com.ahmetkaragunlu.financeai.feature.location.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.coroutines.di.IoDispatcher
import com.ahmetkaragunlu.financeai.feature.location.domain.AddressResolver
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidAddressResolver @Inject constructor(
    @ApplicationContext private val context: Context, @IoDispatcher private val io: CoroutineDispatcher
) : AddressResolver {
    override suspend fun resolve(coordinates: Coordinates): LocationData? {
        val addresses = request(coordinates, null)
        return addresses?.firstOrNull()?.let(::parse)
    }
    override suspend fun search(query: String): Coordinates? = request(null, query)?.firstOrNull()?.let { Coordinates(it.latitude, it.longitude) }

    private suspend fun request(coordinates: Coordinates?, query: String?): List<Address>? = try {
        withTimeoutOrNull(10_000) {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { continuation ->
                val terminal = AtomicBoolean(false)
                fun finish(addresses: List<Address>?) {
                    if (terminal.compareAndSet(false, true)) continuation.resume(addresses)
                }
                continuation.invokeOnCancellation { terminal.set(true) }
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = finish(addresses)
                    override fun onError(errorMessage: String?) = finish(null)
                }
                if (coordinates != null) geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1, listener)
                else geocoder.getFromLocationName(checkNotNull(query), 1, listener)
            } else withContext(io) {
                @Suppress("DEPRECATION")
                if (coordinates != null) geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)
                else geocoder.getFromLocationName(checkNotNull(query), 1)
            }
        }
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) { null }

    private fun parse(address: Address): LocationData {
        val full = buildString {
            address.thoroughfare?.let { append("$it ") }
            address.subThoroughfare?.let { append("No:$it ") }
            address.subLocality?.let { append(", $it") }
            address.featureName?.let { if (it != address.thoroughfare && it != address.subLocality) append(", $it") }
            address.adminArea?.let { append(", $it") }
            address.countryName?.let { append(", $it") }
        }.trim()
        val short = buildString {
            val district = address.subLocality ?: address.locality ?: address.subAdminArea
            district?.let { append(it) }
            if (district != null && address.adminArea != null) append(", ")
            address.adminArea?.let { append(it) }
        }
        return LocationData(address.latitude, address.longitude, full.ifBlank { context.getString(R.string.location_info_unavailable) },
            short.ifBlank { context.getString(R.string.location_label) })
    }
}

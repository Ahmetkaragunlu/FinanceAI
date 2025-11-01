package com.ahmetkaragunlu.financeai.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.ahmetkaragunlu.financeai.R
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtil {

    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): LocationData? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        continuation.resume(address?.let { parseAddress(context, it) })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { parseAddress(context, it) }
            }
        } catch (e: Exception) {
            LocationData(
                latitude = latitude,
                longitude = longitude,
                addressFull = context.getString(R.string.location_coordinates, latitude, longitude),
                addressShort = context.getString(R.string.location_label)
            )
        }
    }

    suspend fun searchLocation(context: Context, query: String): LatLng? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        continuation.resume(
                            address?.let { LatLng(it.latitude, it.longitude) }
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                addresses?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAddress(context: Context, address: Address): LocationData {
        val fullAddress = buildString {
            address.thoroughfare?.let { append("$it ") }
            address.subThoroughfare?.let { append("No:$it ") }
            address.subLocality?.let { append(", $it") }
            address.featureName?.let {
                if (it != address.thoroughfare && it != address.subLocality) {
                    append(", $it")
                }
            }
            address.adminArea?.let { append(", $it") }
            address.countryName?.let { append(", $it") }
        }.trim()

        val shortAddress = buildString {
            val district = address.subLocality ?: address.locality ?: address.subAdminArea
            val city = address.adminArea

            district?.let { append(it) }
            if (district != null && city != null) append(", ")
            city?.let { append(it) }

            if (isEmpty()) {
                address.adminArea?.let { append(it) } ?: append(context.getString(R.string.location_label))
            }
        }

        return LocationData(
            latitude = address.latitude,
            longitude = address.longitude,
            addressFull = fullAddress.ifBlank { context.getString(R.string.location_info_unavailable) },
            addressShort = shortAddress.ifBlank { context.getString(R.string.location_label) }
        )
    }
}
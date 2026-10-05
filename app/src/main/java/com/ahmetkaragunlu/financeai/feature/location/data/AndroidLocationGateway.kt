package com.ahmetkaragunlu.financeai.feature.location.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates
import com.ahmetkaragunlu.financeai.feature.location.domain.LocationGateway
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class AndroidLocationGateway @Inject constructor(@ApplicationContext private val context: Context) : LocationGateway {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    override fun hasPermission() = granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)
    override fun isEnabled() = context.getSystemService(LocationManager::class.java).isLocationEnabled
    @SuppressLint("MissingPermission")
    override suspend fun current(): Coordinates? {
        if (!hasPermission() || !isEnabled()) return null
        val cancellation = CancellationTokenSource()
        return try {
            withTimeoutOrNull(15_000) {
                val priority = if (granted(Manifest.permission.ACCESS_FINE_LOCATION)) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                client.getCurrentLocation(priority, cancellation.token).await()?.let { Coordinates(it.latitude, it.longitude) }
            }
        } finally { cancellation.cancel() }
    }
}

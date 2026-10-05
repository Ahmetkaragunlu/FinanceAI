package com.ahmetkaragunlu.financeai.feature.location.domain

import com.ahmetkaragunlu.financeai.feature.location.domain.model.LocationData

data class Coordinates(val latitude: Double, val longitude: Double)

interface LocationGateway {
    fun hasPermission(): Boolean
    fun isEnabled(): Boolean
    suspend fun current(): Coordinates?
}
interface AddressResolver {
    suspend fun resolve(coordinates: Coordinates): LocationData?
    suspend fun search(query: String): Coordinates?
}

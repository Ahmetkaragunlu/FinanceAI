package com.ahmetkaragunlu.financeai.feature.location.domain.model


data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val addressFull: String,
    val addressShort: String
)

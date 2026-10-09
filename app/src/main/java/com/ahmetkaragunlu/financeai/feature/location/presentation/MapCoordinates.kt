package com.ahmetkaragunlu.financeai.feature.location.presentation

import com.ahmetkaragunlu.financeai.feature.location.domain.Coordinates

/** Preserve Maps' existing latitude clamp and longitude wrap without carrying SDK types in state. */
internal fun Coordinates.normalizedForMap(): Coordinates = Coordinates(
    latitude.coerceIn(-90.0, 90.0),
    if (longitude >= -180.0 && longitude < 180.0) longitude
    else ((longitude - 180.0) % 360.0 + 360.0) % 360.0 - 180.0,
)

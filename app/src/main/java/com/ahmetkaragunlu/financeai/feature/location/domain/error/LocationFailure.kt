package com.ahmetkaragunlu.financeai.feature.location.domain.error

sealed interface LocationFailure {
    data object PermissionDenied : LocationFailure

    data object ServicesDisabled : LocationFailure

    data object PositionUnavailable : LocationFailure

    data object AddressNotFound : LocationFailure
}

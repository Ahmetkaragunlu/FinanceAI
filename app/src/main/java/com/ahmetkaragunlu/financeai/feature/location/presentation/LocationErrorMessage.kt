package com.ahmetkaragunlu.financeai.feature.location.presentation

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.feature.location.domain.error.LocationFailure

@StringRes
fun LocationFailure.messageRes(): Int =
    when (this) {
        LocationFailure.PermissionDenied -> R.string.location_permission_required
        LocationFailure.ServicesDisabled -> R.string.gps_disabled
        LocationFailure.PositionUnavailable -> R.string.location_not_available
        LocationFailure.AddressNotFound -> R.string.address_not_found
    }

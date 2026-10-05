package com.ahmetkaragunlu.financeai.core.ui.error

import androidx.annotation.StringRes
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.error.DataAccessException
import kotlinx.coroutines.CancellationException

/** Presentation owns localisation; neither SDK diagnostics nor Throwable.message reach the UI. */
@StringRes
fun dataErrorMessageRes(error: Throwable): Int? = when (error) {
    is CancellationException -> throw error
    is DataAccessException.NetworkUnavailable -> R.string.error_network_unavailable
    is DataAccessException.AccessDenied -> R.string.error_access_denied
    is DataAccessException.RateLimited -> R.string.error_rate_limited
    is DataAccessException.StaleRecord -> R.string.error_stale_record
    else -> null
}

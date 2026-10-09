package com.ahmetkaragunlu.financeai.feature.location.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R

/** Localise only the display fallback; resolved addresses remain unchanged. */
@Composable
internal fun LocationPickerUiState.addressDisplayText(): String? =
    addressText ?: fallbackCoordinates?.let {
        stringResource(R.string.location_coordinates, it.latitude, it.longitude)
    }

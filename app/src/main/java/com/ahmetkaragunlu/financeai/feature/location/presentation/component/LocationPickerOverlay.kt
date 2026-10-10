package com.ahmetkaragunlu.financeai.feature.location.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import androidx.compose.foundation.layout.BoxScope
import com.ahmetkaragunlu.financeai.feature.location.presentation.LocationPickerUiState
import com.ahmetkaragunlu.financeai.feature.location.presentation.addressDisplayText

@Composable
internal fun BoxScope.LocationPickerOverlay(
    uiState: LocationPickerUiState,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    if (uiState.isLoading || uiState.isSearching) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }

    val addressText = uiState.addressDisplayText()
    addressText?.let { address ->
        Card(
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(Spacing.screenPadding),
            colors = CardDefaults.cardColors(containerColor = FinanceColors.dialogSurface),
        ) {
            Text(
                text = address,
                modifier = Modifier.padding(Spacing.screenPadding),
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.onAccent,
            )
        }
        Button(
            onClick = {
                uiState.selectedLocation?.let { location ->
                    onLocationSelected(location.latitude, location.longitude)
                    onDismiss()
                }
            },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.background)
                ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.confirm_location))
        }
    }
}

package com.ahmetkaragunlu.financeai.feature.location.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R

@Composable
internal fun LocationSettingsDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        icon = { Icon(Icons.Default.LocationOff, null) },
        title = { Text(stringResource(R.string.location_services_disabled)) },
        text = { Text(stringResource(R.string.location_services_disabled_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onOpenSettings()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

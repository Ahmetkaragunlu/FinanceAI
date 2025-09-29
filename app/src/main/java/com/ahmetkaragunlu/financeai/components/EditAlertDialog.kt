package com.ahmetkaragunlu.financeai.components

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlertDialog(
    @StringRes title: Int,
    @StringRes text: Int,
    confirmButton: @Composable () -> Unit,
    onDismissRequest : () -> Unit = {}
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = title)) },
        text = { Text(stringResource(id = text)) },
        confirmButton = confirmButton,
    )
}

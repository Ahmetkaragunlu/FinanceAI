package com.ahmetkaragunlu.financeai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable

object TextFieldStyles {
    @Composable
    fun whiteTextFieldColors() = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary,
        focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
}
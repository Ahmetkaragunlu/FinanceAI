package com.ahmetkaragunlu.financeai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SignUpTextFieldStyles {
    @Composable
    fun whiteTextFieldColors() = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary,
        focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
}

object AddTransactionScreenTextFieldStyles {
    @Composable
    fun textFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF404349),
        unfocusedContainerColor = Color(0xFF404349),
        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
        focusedBorderColor = Color(0xFF404349),
        unfocusedBorderColor = Color(0xFF404349),
        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,

    )
}
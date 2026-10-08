package com.ahmetkaragunlu.financeai.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable

object SignUpTextFieldStyles {
    @Composable
    fun whiteTextFieldColors() =
        TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary,
            focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
            focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
        )
}

object AddTransactionScreenTextFieldStyles {
    @Composable
    fun textFieldColors() =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FinanceColors.fieldSurface,
            unfocusedContainerColor = FinanceColors.fieldSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            focusedBorderColor = FinanceColors.fieldSurface,
            unfocusedBorderColor = FinanceColors.fieldSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
        )
}

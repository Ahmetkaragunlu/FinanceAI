package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors

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

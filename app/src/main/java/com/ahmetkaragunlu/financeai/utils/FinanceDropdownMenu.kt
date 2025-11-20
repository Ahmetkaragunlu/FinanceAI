package com.ahmetkaragunlu.financeai.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.ahmetkaragunlu.financeai.R

@Composable
fun <T> FinanceDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    itemLabel: @Composable (T) -> String,
    trigger: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        trigger()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(colorResource(R.color.background))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(option),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}
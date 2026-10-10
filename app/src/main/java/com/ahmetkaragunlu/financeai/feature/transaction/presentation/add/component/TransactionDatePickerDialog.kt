package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDatePickerDialog(
    initialSelectedDateMillis: Long?,
    isDateValid: (Long) -> Boolean,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return isDateValid(utcTimeMillis)
                    }
                },
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = FinanceColors.dialogSurface),
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        if (isDateValid(timestamp)) {
                            onDateSelected(timestamp)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(id = R.string.cancel),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            colors =
                DatePickerDefaults.colors(
                    containerColor = FinanceColors.dialogSurface,
                    dayContentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledDayContentColor = FinanceColors.mutedText,
                    weekdayContentColor = MaterialTheme.colorScheme.onPrimary,
                    dividerColor = FinanceColors.dialogSurface,
                    navigationContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    headlineContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = FinanceColors.mutedText,
                    todayDateBorderColor = FinanceColors.mutedText,
                    todayContentColor = FinanceColors.mutedText,
                ),
        )
    }
}

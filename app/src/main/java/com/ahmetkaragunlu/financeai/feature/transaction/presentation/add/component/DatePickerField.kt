package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.core.format.formatAsDate
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import java.time.ZoneId

@Composable
fun DatePickerField(
    selectedDate: Long,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    isReminderEnabled: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val formattedDate = selectedDate.formatAsDate(zone = zone)

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (isReminderEnabled) FinanceColors.onAccent else FinanceColors.mutedText,
            )
        },
        modifier = modifier.clickable { onDateClick() },
        colors =
            OutlinedTextFieldDefaults.colors(
                disabledContainerColor = FinanceColors.fieldSurface,
                disabledTextColor =
                    if (isReminderEnabled) FinanceColors.onAccent else FinanceColors.mutedText,
            ),
        enabled = false,
        shape = RoundedCornerShape(12.dp),
    )
}

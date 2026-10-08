package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing

@Composable
fun ReminderSwitch(isEnabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .background(color = FinanceColors.fieldSurface, shape = RoundedCornerShape(12.dp))
                .padding(Spacing.screenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.reminder_switch_title),
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text =
                    stringResource(
                        id =
                            if (isEnabled) R.string.reminder_enabled_desc
                            else R.string.reminder_disabled_desc
                    ),
                color = FinanceColors.mutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = FinanceColors.onAccent,
                    checkedTrackColor = FinanceColors.suggestionStart,
                    uncheckedThumbColor = FinanceColors.mutedText,
                    uncheckedTrackColor = FinanceColors.inactiveTrack,
                ),
        )
    }
}

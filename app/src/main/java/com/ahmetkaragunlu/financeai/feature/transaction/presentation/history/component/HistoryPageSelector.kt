package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing

@Composable
internal fun HistoryPageSelector(
    isHistoryPage: Boolean,
    onPageSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.widthIn(max = 400.dp).fillMaxWidth().padding(Spacing.screenPadding)
    ) {
        OutlinedButton(
            onClick = { onPageSelected(true) },
            modifier = modifier.weight(1f),
            colors =
                if (isHistoryPage) {
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
        ) {
            Text(
                text = stringResource(R.string.history),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = modifier.width(16.dp))

        OutlinedButton(
            onClick = { onPageSelected(false) },
            modifier = modifier.weight(1f),
            colors =
                if (!isHistoryPage) {
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
        ) {
            Text(
                text = stringResource(R.string.scheduled),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

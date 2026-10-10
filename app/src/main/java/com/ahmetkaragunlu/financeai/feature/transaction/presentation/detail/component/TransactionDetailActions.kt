package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing

@Composable
internal fun TransactionDetailActions(
    onEditRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
    ) {
        Button(
            onClick = onEditRequested,
            modifier = modifier.weight(1f).height(50.dp),
            colors =
                ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = stringResource(R.string.edit),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Button(
            onClick = onDeleteRequested,
            modifier = modifier.weight(1f).height(50.dp),
            colors =
                ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = stringResource(R.string.delete),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

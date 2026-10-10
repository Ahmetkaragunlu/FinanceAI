package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

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
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType

@Composable
internal fun TransactionTypeSelector(
    type: TransactionType,
    onTypeChanged: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(Spacing.screenPadding).widthIn(max = 400.dp).fillMaxWidth()) {
        OutlinedButton(
            onClick = { onTypeChanged(TransactionType.EXPENSE) },
            modifier = modifier.weight(1f),
            colors =
                if (type == TransactionType.EXPENSE) {
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                } else ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(
                text = stringResource(R.string.expense),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = modifier.width(16.dp))
        OutlinedButton(
            onClick = { onTypeChanged(TransactionType.INCOME) },
            modifier = Modifier.weight(1f),
            colors =
                if (type == TransactionType.INCOME) {
                    ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                } else ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(
                text = stringResource(R.string.income),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

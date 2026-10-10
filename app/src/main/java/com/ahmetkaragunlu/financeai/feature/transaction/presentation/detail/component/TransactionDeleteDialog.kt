package com.ahmetkaragunlu.financeai.feature.transaction.presentation.detail.component

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.EditAlertDialog
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors

@Composable
internal fun TransactionDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    EditAlertDialog(
        title = R.string.delete_transaction_title,
        text = R.string.delete_transaction_message,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = FinanceColors.expense)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

package com.ahmetkaragunlu.financeai.app.presentation.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.DateFormatter
import com.ahmetkaragunlu.financeai.core.media.PhotoFields
import com.ahmetkaragunlu.financeai.core.sync.SyncPayload
import com.ahmetkaragunlu.financeai.core.sync.contract.FinancialFields
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.schedule.data.remote.ScheduleFields
import com.ahmetkaragunlu.financeai.feature.transaction.data.remote.TransactionFields
import com.ahmetkaragunlu.financeai.feature.transaction.format.toResId

@Composable
fun SyncConflictDialog(viewModel: SyncConflictViewModel = hiltViewModel()) {
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val conflict = conflicts.firstOrNull() ?: return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.sync_conflict_title)) },
        text = {
            Column {
                Text(stringResource(R.string.sync_conflict_description))
                Text(stringResource(R.string.sync_conflict_local))
                ConflictSummary(if (conflict.pendingDelete) null else conflict.pendingPayload)
                Text(stringResource(R.string.sync_conflict_remote))
                ConflictSummary(conflict.conflictPayload)
                if (error) Text(stringResource(R.string.sync_conflict_error))
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.resolve(conflict, true) }) {
            Text(stringResource(R.string.sync_keep_local))
        } },
        dismissButton = { TextButton(enabled = !busy, onClick = { viewModel.resolve(conflict, false) }) {
            Text(stringResource(R.string.sync_keep_remote))
        } }
    )
}

@Composable
private fun ConflictSummary(payload: String?) {
    if (payload == null) { Text(stringResource(R.string.sync_deleted_record)); return }
    val values = SyncPayload.decode(payload)
    values["limitPercentage"]?.let { Text(stringResource(R.string.percentage_label) + ": " + it) }
    (values[TransactionFields.DATE] ?: values[ScheduleFields.DATE])?.let { timestamp ->
        Text(DateFormatter.formatRelativeDate(LocalContext.current, (timestamp as Number).toLong()))
    }
    values[FinancialFields.LOCATION_FULL]?.toString()?.let { Text(stringResource(R.string.location_with_value, it)) }
    values[TransactionFields.TYPE]?.let { Text(stringResource(if (it == "INCOME") R.string.income else R.string.expense)) }
    values[ScheduleFields.TYPE]?.let { Text(stringResource(if (it == "INCOME") R.string.income else R.string.expense)) }
    if (values[PhotoFields.REMOVED] == true) Text(stringResource(R.string.sync_photo_removed))
    else if (values[PhotoFields.STORAGE_URL] != null) Text(stringResource(R.string.sync_photo_attached))
    values[FinancialFields.LEGACY_AMOUNT]?.let { Text("$it ${values[FinancialFields.CURRENCY_CODE] ?: ""}") }
    (values["note"] ?: values["text"])?.toString()?.takeIf { it.isNotBlank() }?.let { Text(it) }
    values["category"]?.toString()?.let { name ->
        val category = CategoryType.valueOf(name)
        Text(stringResource(category.toResId()))
    }
}

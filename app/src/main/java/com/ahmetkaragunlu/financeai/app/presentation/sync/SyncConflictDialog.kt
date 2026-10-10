package com.ahmetkaragunlu.financeai.app.presentation.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.DateFormatter

@Composable
fun SyncConflictDialog(
    viewModel: SyncConflictViewModel = hiltViewModel()
) {
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
        confirmButton = {
            TextButton(enabled = !busy, onClick = { viewModel.resolve(conflict, true) }) {
                Text(stringResource(R.string.sync_keep_local))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { viewModel.resolve(conflict, false) }) {
                Text(stringResource(R.string.sync_keep_remote))
            }
        }
    )
}

@Composable
private fun ConflictSummary(payload: String?) {
    for (line in mapConflictSummary(payload)) {
        when (line) {
            is ConflictSummaryLine.Value -> Text(line.text)
            is ConflictSummaryLine.Label -> Text(stringResource(line.resourceId))
            is ConflictSummaryLine.FormattedLabel ->
                Text(stringResource(line.resourceId, line.argument))
            is ConflictSummaryLine.Date ->
                Text(DateFormatter.formatRelativeDate(LocalContext.current, line.timestamp))
            is ConflictSummaryLine.Percentage ->
                Text(stringResource(R.string.percentage_label) + ": " + line.value)
        }
    }
}

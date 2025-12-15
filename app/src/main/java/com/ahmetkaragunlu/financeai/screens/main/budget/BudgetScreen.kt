package com.ahmetkaragunlu.financeai.screens.main.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditAlertDialog

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteDialogState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background))
    ) {
        if (uiState.isBudgetEmpty) {
            EmptyBudgetContent(
                onCreateGeneralClick = { viewModel.onEvent(BudgetEvent.OnCreateGeneralBudgetClick) },
                onAddLimitClick = { viewModel.onEvent(BudgetEvent.OnAddBudgetClick) }
            )
        } else {
            FilledBudgetContent(
                uiState = uiState,
                onEvent = viewModel::onEvent
            )
        }
        if (formState.isVisible) {
            val isGeneralBudgetSet = (uiState.generalBudgetState?.limitAmount ?: 0.0) > 0
            AddBudgetBottomSheet(
                formState = formState,
                isGeneralBudgetSet = isGeneralBudgetSet,
                onEvent = viewModel::onEvent
            )
        }

        if (deleteState.isVisible) {
            EditAlertDialog(
                title = R.string.delete,
                text = R.string.delete_transaction_message,
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(BudgetEvent.OnConfirmDelete) }) {
                        Text(stringResource(R.string.delete), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(BudgetEvent.OnDismissDeleteDialog) }) {
                        Text(stringResource(R.string.cancel), color = Color.Gray)
                    }
                },
                onDismissRequest = { viewModel.onEvent(BudgetEvent.OnDismissDeleteDialog) }
            )
        }
        if (formState.isConflictDialogOpen) {
            EditAlertDialog(
                onDismissRequest = { viewModel.onEvent(BudgetEvent.OnDismissConflictDialog) },
                title = R.string.warning,
                text = formState.conflictErrorResId ?: R.string.budget_rule_exists_error,
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(BudgetEvent.OnDismissConflictDialog) }) {
                        Text(stringResource(R.string.ok), color = Color.Gray)
                    }
                }
            )
        }
    }
}
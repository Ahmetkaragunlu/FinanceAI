package com.ahmetkaragunlu.financeai.feature.budget.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.EditAlertDialog
import com.ahmetkaragunlu.financeai.core.ui.effect.ToastMessageEffect
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors

@Composable
fun BudgetRoute(modifier: Modifier = Modifier, viewModel: BudgetViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteDialogState.collectAsStateWithLifecycle()
    val errorResId by viewModel.errorResId.collectAsStateWithLifecycle()
    ToastMessageEffect(errorResId, viewModel::consumeError)

    BudgetScreen(uiState, formState, deleteState, viewModel::onEvent, modifier)
}

@Composable
fun BudgetScreen(
    uiState: BudgetUiState,
    formState: BudgetFormState,
    deleteState: DeleteDialogState,
    onEvent: (BudgetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(colorResource(R.color.background))) {
        if (uiState.isBudgetEmpty) {
            EmptyBudgetContent(
                onCreateGeneralClick = { onEvent(BudgetEvent.OnCreateGeneralBudgetClick) },
                onAddLimitClick = { onEvent(BudgetEvent.OnAddBudgetClick) },
            )
        } else {
            FilledBudgetContent(uiState = uiState, onEvent = onEvent)
        }
        if (formState.isVisible) {
            val isGeneralBudgetSet = (uiState.generalBudgetState?.limitAmount ?: 0.0) > 0
            AddBudgetBottomSheet(
                formState = formState,
                isGeneralBudgetSet = isGeneralBudgetSet,
                onEvent = onEvent,
            )
        }

        if (deleteState.isVisible) {
            EditAlertDialog(
                title = R.string.delete,
                text = R.string.delete_budget_message,
                confirmButton = {
                    TextButton(onClick = { onEvent(BudgetEvent.OnConfirmDelete) }) {
                        Text(stringResource(R.string.delete), color = FinanceColors.expense)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(BudgetEvent.OnDismissDeleteDialog) }) {
                        Text(stringResource(R.string.cancel), color = FinanceColors.mutedText)
                    }
                },
                onDismissRequest = { onEvent(BudgetEvent.OnDismissDeleteDialog) },
            )
        }
        if (formState.isConflictDialogOpen) {
            EditAlertDialog(
                onDismissRequest = { onEvent(BudgetEvent.OnDismissConflictDialog) },
                title = R.string.warning,
                text = formState.conflictErrorResId ?: R.string.budget_rule_exists_error,
                confirmButton = {
                    TextButton(onClick = { onEvent(BudgetEvent.OnDismissConflictDialog) }) {
                        Text(stringResource(R.string.ok), color = FinanceColors.mutedText)
                    }
                },
            )
        }
    }
}

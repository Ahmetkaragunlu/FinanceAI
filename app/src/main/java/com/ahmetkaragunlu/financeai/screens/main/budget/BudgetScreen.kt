package com.ahmetkaragunlu.financeai.screens.main.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditAlertDialog
import com.ahmetkaragunlu.financeai.viewmodel.BudgetViewModel

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background))
    ) {
        when {
            uiState.isBudgetEmpty -> {
                EmptyBudgetContent(
                    onAddLimitClick = viewModel::openCreateGeneralBudgetSheet
                )
            }

            else -> {
                FilledBudgetContent(
                    uiState = uiState,
                    onAddLimitClick = viewModel::openAddBudgetSheet,
                    onEditGeneralClick = viewModel::openEditGeneralBudgetSheet,
                    onEditCategoryClick = viewModel::openEditCategoryBudgetSheet,
                    onDeleteCategoryClick = viewModel::openDeleteDialog
                )
            }
        }

        if (viewModel.showBottomSheet) {
            AddBudgetBottomSheet(
                viewModel = viewModel,
                onDismiss = viewModel::closeBottomSheet
            )
        }

        if (viewModel.showDeleteDialog) {
            EditAlertDialog(
                title = R.string.delete,
                text = R.string.delete_transaction_message,
                confirmButton = {
                    TextButton(onClick = viewModel::deleteBudgetRule) {
                        Text(stringResource(R.string.delete), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::closeDeleteDialog) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                },
                onDismissRequest = viewModel::closeDeleteDialog
            )
        }

        if (viewModel.showConflictDialog) {
            EditAlertDialog(
                onDismissRequest = viewModel::closeConflictDialog,
                title = R.string.warning,
                text = R.string.budget_rule_exists_error,
                confirmButton = {
                    TextButton(onClick = viewModel::closeConflictDialog) {
                        Text(stringResource(R.string.ok), color = Color.White)
                    }
                }
            )
        }
    }
}
package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.feature.schedule.presentation.ScheduledTransactionRoute

@Composable
fun TransactionHistoryRoute(
    onTransactionClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    TransactionHistoryScreen(
        transactions = transactions,
        filters = filters,
        showCategoryError = viewModel.showCategoryError,
        onDateSelected = viewModel::onDateSelected,
        onTypeSelected = viewModel::onTypeSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onCategoryMenuRequested = viewModel::canOpenCategoryMenu,
        onTransactionClick = onTransactionClick,
        scheduledContent = { ScheduledTransactionRoute() },
        modifier = modifier,
    )
}

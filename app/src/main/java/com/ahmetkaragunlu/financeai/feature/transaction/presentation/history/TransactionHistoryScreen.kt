package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.time.DateFilter
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component.HistoryFilterBar
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component.HistoryPageSelector
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component.TransactionCard

@Composable
fun TransactionHistoryScreen(
    transactions: List<Transaction>,
    filters: HistoryFilters,
    showCategoryError: Boolean,
    onDateSelected: (DateFilter) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onCategorySelected: (CategoryType?) -> Unit,
    onCategoryMenuRequested: () -> Boolean,
    onTransactionClick: (Int) -> Unit,
    scheduledContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHistoryPage by rememberSaveable { mutableStateOf(true) }
    val contentStateHolder = rememberSaveableStateHolder()
    Column(
        modifier = modifier.fillMaxSize().background(colorResource(R.color.background)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HistoryPageSelector(
            isHistoryPage = isHistoryPage,
            onPageSelected = { isHistoryPage = it },
            modifier = modifier,
        )
        if (isHistoryPage) {
            contentStateHolder.SaveableStateProvider("history") {
                HistoryContent(
                    filters = filters,
                    showCategoryError = showCategoryError,
                    onDateSelected = onDateSelected,
                    onTypeSelected = onTypeSelected,
                    onCategorySelected = onCategorySelected,
                    onCategoryMenuRequested = onCategoryMenuRequested,
                    transactions = transactions,
                    onTransactionClick = onTransactionClick,
                    modifier = modifier,
                )
            }
        } else {
            scheduledContent()
        }
    }
}

@Composable
private fun HistoryContent(
    filters: HistoryFilters,
    showCategoryError: Boolean,
    onDateSelected: (DateFilter) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onCategorySelected: (CategoryType?) -> Unit,
    onCategoryMenuRequested: () -> Boolean,
    transactions: List<Transaction>,
    onTransactionClick: (Int) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        HistoryFilterBar(
            filters = filters,
            showCategoryError = showCategoryError,
            onDateSelected = onDateSelected,
            onTypeSelected = onTypeSelected,
            onCategorySelected = onCategorySelected,
            onCategoryMenuRequested = onCategoryMenuRequested,
            modifier = modifier,
        )
        Spacer(modifier = modifier.height(32.dp))
        LazyColumn(
            modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionCard(transaction = transaction, onTransactionClick = onTransactionClick)
            }
            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_record_found),
                        color = FinanceColors.mutedText,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.screenPadding),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

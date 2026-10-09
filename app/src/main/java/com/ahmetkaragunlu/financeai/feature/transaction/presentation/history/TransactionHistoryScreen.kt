package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.format.formatRelativeDate
import com.ahmetkaragunlu.financeai.core.time.DateFilter
import com.ahmetkaragunlu.financeai.core.ui.component.EditButton
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.schedule.presentation.ScheduledTransactionRoute
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.Transaction
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.format.toResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toIconResId

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
        modifier = modifier,
    )
}

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
    modifier: Modifier = Modifier,
) {
    var isHistoryPage by rememberSaveable { mutableStateOf(true) }
    val contentStateHolder = rememberSaveableStateHolder()
    Column(
        modifier = modifier.fillMaxSize().background(colorResource(R.color.background)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = modifier.widthIn(max = 400.dp).fillMaxWidth().padding(Spacing.screenPadding)
        ) {
            OutlinedButton(
                onClick = { isHistoryPage = true },
                modifier = modifier.weight(1f),
                colors =
                    if (isHistoryPage) {
                        ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
            ) {
                Text(
                    text = stringResource(R.string.history),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = modifier.width(16.dp))

            OutlinedButton(
                onClick = { isHistoryPage = false },
                modifier = modifier.weight(1f),
                colors =
                    if (!isHistoryPage) {
                        ButtonDefaults.buttonColors(containerColor = FinanceColors.fieldSurface)
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
            ) {
                Text(
                    text = stringResource(R.string.scheduled),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
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
            ScheduledTransactionRoute()
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
    var isDateMenuOpen by rememberSaveable { mutableStateOf(false) }
    var isTypeMenuOpen by rememberSaveable { mutableStateOf(false) }
    var isCategoryMenuOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier =
                modifier
                    .padding(horizontal = Spacing.screenPadding)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            FinanceDropdownMenu(
                modifier = modifier.weight(1f),
                expanded = isDateMenuOpen,
                onExpandedChange = { isDateMenuOpen = it },
                options = DateFilter.entries,
                onOptionSelected = { id ->
                    onDateSelected(id)
                    isDateMenuOpen = false
                },
                itemLabel = { date -> stringResource(date.labelRes()) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = filters.date.labelRes(),
                        icon = R.drawable.calendar,
                        onClick = { isDateMenuOpen = true },
                    )
                },
            )
            Spacer(modifier = modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                FinanceDropdownMenu(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = isCategoryMenuOpen,
                    onExpandedChange = { if (!it) isCategoryMenuOpen = false },
                    options = CategoryType.entries.filter { it.type == filters.type },
                    onOptionSelected = { category ->
                        onCategorySelected(category)
                        isCategoryMenuOpen = false
                    },
                    itemLabel = { category -> stringResource(category.toResId()) },
                    trigger = {
                        EditButton(
                            modifier = Modifier.fillMaxWidth(),
                            label = filters.category?.toResId() ?: R.string.category,
                            icon = R.drawable.categories,
                            onClick = { isCategoryMenuOpen = onCategoryMenuRequested() },
                        )
                    },
                )
                if (showCategoryError) {
                    Text(
                        text = stringResource(R.string.error_select_type_first),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }
            }
            Spacer(modifier = modifier.width(8.dp))
            FinanceDropdownMenu(
                modifier = modifier.weight(1f),
                expanded = isTypeMenuOpen,
                onExpandedChange = { isTypeMenuOpen = it },
                options = TransactionType.entries,
                onOptionSelected = { type ->
                    onTypeSelected(type)
                    isTypeMenuOpen = false
                },
                itemLabel = { type -> stringResource(type.labelRes()) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = filters.type?.labelRes() ?: R.string.type,
                        icon = R.drawable.type,
                        onClick = { isTypeMenuOpen = true },
                    )
                },
            )
        }
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

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
    }

@Composable
private fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onTransactionClick: (Int) -> Unit,
) {
    val context = LocalContext.current

    Card(
        onClick = { onTransactionClick(transaction.id) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(
                        brush = FinanceGradients.financialCard,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(Spacing.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = Color.Transparent) {
                Icon(
                    painter = painterResource(transaction.category.toIconResId()),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = modifier.padding(8.dp),
                )
            }

            Spacer(modifier = modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(transaction.category.toResId()),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = transaction.date.formatRelativeDate(context),
                    color = FinanceColors.mutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = modifier.weight(1f))

            val amountColor =
                if (transaction.transaction == TransactionType.INCOME) FinanceColors.income
                else FinanceColors.expense

            Text(text = transaction.amount.formatAsAccountCurrency(), color = amountColor)
            Spacer(modifier = modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.LightGray,
            )
        }
    }
}

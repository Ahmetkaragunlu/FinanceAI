package com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.time.DateFilter
import com.ahmetkaragunlu.financeai.core.ui.component.EditButton
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.HistoryFilters
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.history.labelRes

@Composable
internal fun HistoryFilterBar(
    filters: HistoryFilters,
    showCategoryError: Boolean,
    onDateSelected: (DateFilter) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onCategorySelected: (CategoryType?) -> Unit,
    onCategoryMenuRequested: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    var isDateMenuOpen by rememberSaveable { mutableStateOf(false) }
    var isTypeMenuOpen by rememberSaveable { mutableStateOf(false) }
    var isCategoryMenuOpen by rememberSaveable { mutableStateOf(false) }
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
                    textTopPadding = if (filters.date == DateFilter.ALL) 3.dp else 0.dp,
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
                itemLabel = { category -> stringResource(category.toLabelResId()) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = filters.category?.toLabelResId() ?: R.string.category,
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
}

private fun TransactionType.labelRes(): Int =
    when (this) {
        TransactionType.INCOME -> R.string.income
        TransactionType.EXPENSE -> R.string.expense
    }

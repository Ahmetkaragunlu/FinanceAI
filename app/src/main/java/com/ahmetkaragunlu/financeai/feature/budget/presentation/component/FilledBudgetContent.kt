package com.ahmetkaragunlu.financeai.feature.budget.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetEvent
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetUiState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper.localizedText
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section.GeneralBudgetSection
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section.CategoryBudgetSection
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section.BudgetWarningCard

@Composable
fun FilledBudgetContent(
    uiState: BudgetUiState,
    onEvent: (BudgetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        GeneralBudgetSection(
            state = uiState.generalBudgetState,
            onEditClick = { state -> onEvent(BudgetEvent.OnEditGeneralClick(state)) },
            onCreateClick = { onEvent(BudgetEvent.OnCreateGeneralBudgetClick) },
        )
        Spacer(modifier = Modifier.height(20.dp))
        uiState.warning?.let { warning ->
            BudgetWarningCard(message = warning.localizedText())
            Spacer(modifier = Modifier.height(24.dp))
        }
        Text(
            text = stringResource(R.string.category_budgets),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .padding(start = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CategoryBudgetSection(categories = uiState.categoryBudgetStates, onEvent = onEvent)
        Spacer(modifier = Modifier.height(20.dp))
        AddLimitButton(onClick = { onEvent(BudgetEvent.OnAddBudgetClick) })
        Spacer(modifier = Modifier.height(100.dp))
    }
}

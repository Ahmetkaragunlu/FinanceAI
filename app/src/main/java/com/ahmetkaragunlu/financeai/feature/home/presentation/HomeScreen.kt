package com.ahmetkaragunlu.financeai.feature.home.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsUiPercentage
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.home.presentation.component.ExpensePieChart
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onAiSuggestionClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val categoryExpenses by viewModel.monthlyCategoryExpenses.collectAsStateWithLifecycle()
    val aiSuggestion by viewModel.aiSuggestion.collectAsStateWithLifecycle()

    // The existing root-screen back policy is explicitly retained.
    BackHandler {}

    HomeScreen(uiState, categoryExpenses.orEmpty(), aiSuggestion, onAiSuggestionClick, modifier)
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    categoryExpenses: List<CategoryExpense>,
    aiSuggestion: AiSuggestionState,
    onAiSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestionText = aiSuggestion.localizedText()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = colorResource(R.color.background))
                .verticalScroll(rememberScrollState())
    ) {
        // Monthly Summary Card
        MonthlySummaryCard(uiState)

        AiSuggestionCard(suggestionText, onClick = { onAiSuggestionClick(suggestionText.prompt) })
        Text(
            text = stringResource(R.string.expense_categories),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = modifier.padding(Spacing.screenPadding),
        )

        ExpensePieChart(categoryExpenses = categoryExpenses)
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FinanceProgressBar(remainingIncomeRatio: Double, modifier: Modifier = Modifier) {
    val progressValue = if (remainingIncomeRatio < 0) 1f else remainingIncomeRatio.toFloat()
    val percentageText = (remainingIncomeRatio * 100).formatAsUiPercentage()

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { progressValue.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            trackColor = FinanceColors.onAccent.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            color = if (remainingIncomeRatio == 0.0) Color.Transparent else FinanceColors.onAccent,
        )
        Text(
            text = percentageText,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun MonthlySummaryCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(brush = FinanceGradients.summary, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(Spacing.screenPadding)) {
            Text(
                text = stringResource(R.string.this_months_summary),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Row(modifier = modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = state.totalIncome,
                    color = FinanceColors.income,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = modifier.weight(1f))
                Text(
                    text = state.totalExpense,
                    color = FinanceColors.expense,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = stringResource(R.string.remaining_budget),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = state.remainingBalanceFormatted,
                color =
                    if (state.remainingBalance >= 0) FinanceColors.income
                    else FinanceColors.expense,
                style = MaterialTheme.typography.titleLarge,
                modifier = modifier.padding(8.dp),
            )
            FinanceProgressBar(remainingIncomeRatio = state.remainingIncomeRatio)
        }
    }
}

@Composable
private fun AiSuggestionCard(
    text: AiSuggestionText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(brush = FinanceGradients.suggestion, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick,
    ) {
        Row(
            modifier = modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(R.drawable.ai_suggestion), contentDescription = null)
            Column(modifier = modifier.padding(8.dp)) {
                Text(
                    text = stringResource(R.string.ai_suggestion),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = text.message,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

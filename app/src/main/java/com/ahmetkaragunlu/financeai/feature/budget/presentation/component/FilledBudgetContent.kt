package com.ahmetkaragunlu.financeai.feature.budget.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsUiPercentage
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing
import com.ahmetkaragunlu.financeai.feature.budget.domain.calculation.BudgetWarningThreshold
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetEvent
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetUiState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.CategoryBudgetState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.GeneralBudgetState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.mapper.localizedText
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toIconResId
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toLabelResId

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
        if (uiState.generalBudgetState != null) {
            GeneralBudgetCard(
                state = uiState.generalBudgetState,
                onEditClick = {
                    onEvent(BudgetEvent.OnEditGeneralClick(uiState.generalBudgetState))
                },
            )
        } else {
            EmptyGeneralBudgetCard(
                onCreateClick = { onEvent(BudgetEvent.OnCreateGeneralBudgetClick) }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        uiState.warning?.let { warning ->
            WarningCard(message = warning.localizedText())
            Spacer(modifier = Modifier.height(24.dp))
        }
        Text(
            text = stringResource(R.string.category_budgets),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp).padding(start = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        CategoryBudgetList(categories = uiState.categoryBudgetStates, onEvent = onEvent)
        Spacer(modifier = Modifier.height(20.dp))
        AddLimitButton(onClick = { onEvent(BudgetEvent.OnAddBudgetClick) })
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun EmptyGeneralBudgetCard(onCreateClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().widthIn(max = 450.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(brush = FinanceGradients.summary, shape = RoundedCornerShape(20.dp))
                    .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.monthly_budget),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(
                        onClick = onCreateClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = FinanceColors.onAccent.copy(alpha = 0.2f)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.create),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.not_set),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.set_monthly_budget_to_start),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun GeneralBudgetCard(
    state: GeneralBudgetState,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val realPercentage =
        if (state.limitAmount > 0) ((state.expenseAmount / state.limitAmount) * 100).toInt() else 0
    val isOverBudget = state.remainingAmount < 0

    val defaultTextColor = MaterialTheme.colorScheme.onPrimary
    val subTextColor = FinanceColors.onAccent.copy(alpha = 0.7f)
    val expenseTextColor = if (isOverBudget) FinanceColors.expense else FinanceColors.expenseMuted
    val percentageTextColor =
        if (isOverBudget) FinanceColors.expense else FinanceColors.onAccent.copy(alpha = 0.9f)
    val progressBarColor = if (isOverBudget) FinanceColors.expense else FinanceColors.onAccent

    Card(
        modifier = modifier.fillMaxWidth().widthIn(max = 450.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(brush = FinanceGradients.summary, shape = RoundedCornerShape(20.dp))
                    .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.this_month_budget),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(
                        onClick = onEditClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = FinanceColors.onAccent.copy(alpha = 0.2f)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.edit),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.set_limit_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor,
                        )
                        Text(
                            text = state.limitAmount.formatAsAccountCurrency(),
                            style = MaterialTheme.typography.titleLarge,
                            color = defaultTextColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.remaining_budget),
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor,
                        )
                        Text(
                            text = state.remainingAmount.formatAsAccountCurrency(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = defaultTextColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SmoothLinearProgress(
                    progress = state.progress,
                    color = progressBarColor,
                    trackColor = FinanceColors.onAccent.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.expense_format,
                                state.expenseAmount.formatAsAccountCurrency(),
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = expenseTextColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.spent_percent_format, realPercentage.formatAsUiPercentage()),
                        style = MaterialTheme.typography.labelLarge,
                        color = percentageTextColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().widthIn(max = 450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush =
                            Brush.linearGradient(
                                listOf(FinanceColors.dangerStart, FinanceColors.dangerEnd)
                            ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(Spacing.screenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FinanceColors.onAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.warning_symbol), fontSize = 24.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.attention_budget_exceeded),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.onAccent.copy(alpha = 0.95f),
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetList(
    categories: List<CategoryBudgetState>,
    onEvent: (BudgetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().widthIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        categories.forEach { state ->
            CategoryBudgetCard(
                state = state,
                onEditClick = { onEvent(BudgetEvent.OnEditCategoryClick(state)) },
                onDeleteClick = { onEvent(BudgetEvent.OnDeleteClick(state.id)) },
            )
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    state: CategoryBudgetState,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressColor =
        when {
            state.isOverBudget -> FinanceColors.expenseMuted
            state.progress > BudgetWarningThreshold.progress -> FinanceColors.warning
            else -> FinanceColors.success
        }

    val limitText =
        if (state.limitPercentage != null && state.limitPercentage > 0) {
            stringResource(
                R.string.limit_percent_format,
                state.limitPercentage.toInt().formatAsUiPercentage(),
                state.limitAmount.formatAsAccountCurrency(),
            )
        } else {
            stringResource(
                R.string.limit_amount_format,
                state.limitAmount.formatAsAccountCurrency(),
            )
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush = FinanceGradients.financialCard,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(Spacing.screenPadding)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FinanceColors.onAccent.copy(alpha = 0.1f),
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = state.category.toIconResId()),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = state.category.toLabelResId()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = limitText,
                            style = MaterialTheme.typography.bodySmall,
                            color = FinanceColors.onAccent.copy(alpha = 0.6f),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = FinanceColors.mutedText.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = FinanceColors.mutedText.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SmoothLinearProgress(
                    progress = state.progress,
                    color = progressColor,
                    trackColor = FinanceColors.onAccent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.spent_vs_limit_format,
                                state.spentAmount.formatAsAccountCurrency(),
                                state.limitAmount.formatAsAccountCurrency(),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = FinanceColors.onAccent.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.percentageUsed.formatAsUiPercentage(),
                            style = MaterialTheme.typography.labelLarge,
                            color = progressColor,
                            fontWeight = FontWeight.Bold,
                        )
                        if (state.isOverBudget) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.warning_symbol), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

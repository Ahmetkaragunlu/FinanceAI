package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.ahmetkaragunlu.financeai.feature.budget.presentation.CategoryBudgetState
import com.ahmetkaragunlu.financeai.feature.transaction.presentation.mapper.toIconResId
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.SmoothLinearProgress

@Composable
internal fun CategoryBudgetSection(
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

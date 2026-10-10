package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.ahmetkaragunlu.financeai.feature.budget.presentation.GeneralBudgetState
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.SmoothLinearProgress

@Composable
internal fun GeneralBudgetSection(
    state: GeneralBudgetState?,
    onEditClick: (GeneralBudgetState) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state != null) {
        GeneralBudgetCard(
            state = state,
            onEditClick = { onEditClick(state) },
            modifier = modifier,
        )
    } else {
        EmptyGeneralBudgetCard(onCreateClick = onCreateClick, modifier = modifier)
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

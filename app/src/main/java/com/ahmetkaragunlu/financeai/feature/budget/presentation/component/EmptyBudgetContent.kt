package com.ahmetkaragunlu.financeai.feature.budget.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceGradients
import com.ahmetkaragunlu.financeai.core.ui.theme.Spacing

@Composable
fun EmptyBudgetContent(
    onCreateGeneralClick: () -> Unit,
    onAddLimitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Monthly Budget Card ---
        Card(
            modifier = modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(
                            brush = FinanceGradients.summary,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(Spacing.screenPadding)
            ) {
                Column(modifier = modifier.fillMaxWidth()) {
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.monthly_budget),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Button(
                            onClick = onCreateGeneralClick,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = FinanceColors.onAccent.copy(alpha = 0.2f)
                                ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = modifier.height(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = modifier.size(14.dp),
                            )
                            Spacer(modifier = modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.create),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(modifier = modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.not_set),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.set_monthly_budget_to_start),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        // --- AI Assistant Card ---
        Card(
            modifier = modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(FinanceColors.adviceStart, FinanceColors.adviceEnd)
                                ),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ai_suggestion),
                        contentDescription = null,
                        tint = FinanceColors.onAdvice,
                    )
                    Column(modifier = modifier.padding(8.dp)) {
                        Text(
                            text = stringResource(R.string.ai_assistant),
                            style = MaterialTheme.typography.titleMedium,
                            color = FinanceColors.onAdvice,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.ai_welcome_message),
                            color = FinanceColors.onAdvice.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }

        // --- Category Title ---
        Text(
            text = stringResource(R.string.category_budgets),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = modifier.fillMaxWidth().padding(Spacing.screenPadding),
        )

        // --- Empty State Message ---
        Column(
            modifier =
                modifier.fillMaxWidth().widthIn(max = 450.dp).padding(top = 12.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.budget_money_symbol),
                fontSize = 80.sp,
                modifier = modifier.padding(bottom = 20.dp),
            )
            Text(
                text = stringResource(R.string.no_budget_rules_yet),
                style = MaterialTheme.typography.titleMedium,
                color = FinanceColors.onAccent.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = modifier.height(12.dp))
            Text(
                text = stringResource(R.string.create_first_budget_rule_description),
                style = MaterialTheme.typography.bodyMedium,
                color = FinanceColors.mutedText,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = modifier.widthIn(max = 280.dp),
            )
        }

        // --- Add Limit Button ---
        AddLimitButton(onClick = onAddLimitClick, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

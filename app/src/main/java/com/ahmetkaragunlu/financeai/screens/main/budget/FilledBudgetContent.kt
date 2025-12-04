package com.ahmetkaragunlu.financeai.screens.main.budget


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import com.ahmetkaragunlu.financeai.utils.formatAsCurrency
import com.ahmetkaragunlu.financeai.utils.toIconResId
import com.ahmetkaragunlu.financeai.utils.toResId
import com.ahmetkaragunlu.financeai.viewmodel.BudgetUiState
import com.ahmetkaragunlu.financeai.viewmodel.CategoryBudgetState
import com.ahmetkaragunlu.financeai.viewmodel.GeneralBudgetState

@Composable
fun FilledBudgetContent(
    uiState: BudgetUiState,
    onAddLimitClick: () -> Unit,
    onEditGeneralClick: (GeneralBudgetState) -> Unit,
    onEditCategoryClick: (CategoryBudgetState) -> Unit,
    onDeleteCategoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        uiState.generalBudgetState?.let { state ->
            GeneralBudgetCard(
                state = state,
                onEditClick = { onEditGeneralClick(state) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        uiState.aiWarningMessage?.let { message ->
            WarningCard(message = message)
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = stringResource(R.string.category_budgets),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        CategoryBudgetList(
            categories = uiState.categoryBudgetStates,
            onEditClick = onEditCategoryClick,
            onDeleteClick = onDeleteCategoryClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        AddLimitButton(onClick = onAddLimitClick)

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun GeneralBudgetCard(
    state: GeneralBudgetState,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val realPercentage =
        if (state.limitAmount > 0) ((state.expenseAmount / state.limitAmount) * 100).toInt() else 0
    val isOverBudget = state.remainingAmount < 0

    val defaultTextColor = Color.White
    val subTextColor = Color.White.copy(alpha = 0.7f)
    val expenseTextColor = if (isOverBudget) Color.Red else Color(0xFFEF5350)
    val percentageTextColor = if (isOverBudget) Color.Red else Color.White.copy(alpha = 0.9f)
    val progressBarColor = if (isOverBudget) Color.Red else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFb55ebf), Color(0xFF36a2cc))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.this_month_budget),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.edit),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Belirlenen Limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                        Text(
                            text = state.limitAmount.formatAsCurrency(),
                            style = MaterialTheme.typography.titleLarge,
                            color = defaultTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.remaining_budget),
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                        Text(
                            text = state.remainingAmount.formatAsCurrency(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = defaultTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SmoothLinearProgress(
                    progress = state.progress,
                    color = progressBarColor,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gider: -${state.expenseAmount.formatAsCurrency()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = expenseTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%$realPercentage Harcandı",
                        style = MaterialTheme.typography.labelLarge,
                        color = percentageTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFF6B6B), Color(0xFFC92A2A))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⚠️", fontSize = 24.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.attention_budget_exceeded),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetList(
    categories: List<CategoryBudgetState>,
    onEditClick: (CategoryBudgetState) -> Unit,
    onDeleteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { state ->
            CategoryBudgetCard(
                state = state,
                onEditClick = { onEditClick(state) },
                onDeleteClick = { onDeleteClick(state.id) }
            )
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    state: CategoryBudgetState,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        state.isOverBudget -> Color(0xFFEF5350)
        state.progress > 0.8f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    val limitText = if (state.limitPercentage != null && state.limitPercentage > 0) {
        "Limit: %${state.limitPercentage.toInt()} (${state.limitAmount.formatAsCurrency()})"
    } else {
        "Limit: ${state.limitAmount.formatAsCurrency()}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF3b4351), Color(0xFF2d3139))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = state.category.toIconResId()),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = state.category.toResId()),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = limitText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = Color.Gray.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SmoothLinearProgress(
                    progress = state.progress,
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.spentAmount.formatAsCurrency()} / ${state.limitAmount.formatAsCurrency()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "%${state.percentageUsed}",
                            style = MaterialTheme.typography.labelLarge,
                            color = progressColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.isOverBudget) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "⚠️", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
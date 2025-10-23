package com.ahmetkaragunlu.financeai.screens.main

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.ExpensePieChart
import com.ahmetkaragunlu.financeai.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.homeUiState.collectAsState()
    val categoryExpenses by viewModel.lastMonthCategoryExpenses.collectAsState()

    BackHandler {}

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background))
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            onClick = {},
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFb55ebf),
                            Color(0xFF36a2cc),
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.this_months_summary),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = uiState.totalIncome,
                        color = Color.Green,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = modifier.weight(1f))
                    Text(
                        text = uiState.totalExpense,
                        color = Color.Red,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    text = stringResource(R.string.remaining_budget),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = uiState.remainingBalanceFormatted,
                    color = if (uiState.remainingBalance >= 0) Color.Green else Color.Red,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = modifier.padding(8.dp)
                )
                FinanceProgressBar(spendingPercentage = uiState.spendingPercentage)
            }
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF26b7c3),
                            Color(0xFF27464e),
                            Color(0xFF27464e),
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            onClick = {}
        ) {
            Row(
                modifier = modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ai_suggestion),
                    contentDescription = null,
                )
                Column(modifier = modifier.padding(8.dp)) {
                    Text(
                        text = stringResource(R.string.ai_suggestion),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Market harcamaların bütçeni yüzde 40 açtı. Tasarruf için tıkla",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.expense_categories),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = modifier.padding(16.dp)
        )

        ExpensePieChart(categoryExpenses = categoryExpenses)

    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FinanceProgressBar(
    spendingPercentage: Double,
    modifier: Modifier = Modifier
) {
    val progressValue = if (spendingPercentage < 0) 1f else spendingPercentage.toFloat()
    val percentageText = String.format("%.0f%%", spendingPercentage * 100)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { progressValue.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            trackColor = Color.White.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            color = if (spendingPercentage == 0.0) Color.Transparent else Color.White
        )

        Text(
            text = percentageText,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
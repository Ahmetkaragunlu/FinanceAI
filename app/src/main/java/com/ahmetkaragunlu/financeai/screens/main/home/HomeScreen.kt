package com.ahmetkaragunlu.financeai.screens.main.home

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.utils.ExpensePieChart
import com.ahmetkaragunlu.financeai.viewmodel.AiViewModel
import com.ahmetkaragunlu.financeai.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
    aiViewModel: AiViewModel = hiltViewModel()
) {
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val categoryExpenses by viewModel.lastMonthCategoryExpenses.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()

    BackHandler {}

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background))
            .verticalScroll(rememberScrollState())
    ) {
        // Monthly Summary Card
        Card(
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

        // ✨ GÜNCEL: Dinamik AI Suggestion Card
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
            onClick = {
                // AI sayfasına gitmeden önce prompt'u "bekleyen" olarak kaydet.
                if (aiSuggestion.aiPrompt.isNotBlank()) {
                    aiViewModel.setPendingPrompt(aiSuggestion.aiPrompt)
                }
                navController.navigate(Screens.AiChatScreen.route) {
                    launchSingleTop = true
                }
            }
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
                        text = aiSuggestion.messageText,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        // Expense Categories Section
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
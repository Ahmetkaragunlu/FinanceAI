package com.ahmetkaragunlu.financeai.screens.main.budget

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

@Composable
fun EmptyBudgetContent(
    onCreateGeneralClick: () -> Unit,
    onAddLimitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Monthly Budget Card ---
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFb55ebf), Color(0xFF36a2cc))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = modifier.fillMaxWidth()) {
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.monthly_budget),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Button(
                            onClick = onCreateGeneralClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = modifier.height(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = modifier.size(14.dp)
                            )
                            Spacer(modifier = modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.create),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.not_set),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.set_monthly_budget_to_start),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        // --- AI Assistant Card ---
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF6D365), Color(0xFFFDA085))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ai_suggestion),
                        contentDescription = null,
                        tint = Color(0xFF5D4037)
                    )
                    Column(modifier = modifier.padding(8.dp)) {
                        Text(
                            text = stringResource(R.string.ai_assistant),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF5D4037),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.ai_welcome_message),
                            color = Color(0xFF5D4037).copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleSmall
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
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // --- Empty State Message ---
        Column(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💰",
                fontSize = 80.sp,
                modifier = modifier.padding(bottom = 20.dp)
            )
            Text(
                text = stringResource(R.string.no_budget_rules_yet),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = modifier.height(12.dp))
            Text(
                text = stringResource(R.string.create_first_budget_rule_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = modifier.widthIn(max = 280.dp)
            )
        }

        // --- Add Limit Button ---
        AddLimitButton(
            onClick = onAddLimitClick,
            modifier = Modifier
                .padding(horizontal = 24.dp)
        )
    }
}
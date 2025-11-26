package com.ahmetkaragunlu.financeai.screens.main.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditButton
import com.ahmetkaragunlu.financeai.navigation.Screens
import com.ahmetkaragunlu.financeai.navigation.navigateSingleTopClear
import com.ahmetkaragunlu.financeai.roomdb.entitiy.TransactionEntity
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.utils.*
import com.ahmetkaragunlu.financeai.viewmodel.TransactionHistoryViewModel

@Composable
fun TransactionHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val transactions by viewModel.transactions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // History / Scheduled Toggle
        Row(
            modifier = modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.isHistoryPage = true },
                modifier = modifier.weight(1f),
                colors = if (viewModel.isHistoryPage) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(
                    text = stringResource(R.string.history),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = modifier.width(16.dp))

            OutlinedButton(
                onClick = { viewModel.isHistoryPage = false },
                modifier = modifier.weight(1f),
                colors = if (!viewModel.isHistoryPage) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(
                    text = stringResource(R.string.scheduled),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Filter Row
        Row(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Date Filter
            FinanceDropdownMenu(
                modifier = modifier.weight(1f),
                expanded = viewModel.isDateMenuOpen,
                onExpandedChange = { viewModel.isDateMenuOpen = it },
                options = viewModel.dateOptions,
                onOptionSelected = { id -> viewModel.onDateSelected(id) },
                itemLabel = { id -> stringResource(id) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = viewModel.selectedDateResId,
                        icon = R.drawable.calendar,
                        onClick = { viewModel.isDateMenuOpen = true }
                    )
                }
            )
            Spacer(modifier = modifier.width(8.dp))

            // Category Filter
            Column(modifier = Modifier.weight(1f)) {
                FinanceDropdownMenu(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = viewModel.isCategoryMenuOpen,
                    onExpandedChange = { if (!it) viewModel.isCategoryMenuOpen = false },
                    options = viewModel.categoryOptions,
                    onOptionSelected = { category -> viewModel.onCategorySelected(category) },
                    itemLabel = { category -> stringResource(category.toResId()) },
                    trigger = {
                        EditButton(
                            modifier = Modifier.fillMaxWidth(),
                            label = viewModel.selectedCategory?.toResId() ?: R.string.category,
                            icon = R.drawable.categories,
                            onClick = { viewModel.onCategoryDropdownClicked() }
                        )
                    }
                )

                if (viewModel.showCategoryError) {
                    Text(
                        text = stringResource(R.string.error_select_type_first),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = modifier.padding(top = 4.dp, start = 2.dp)
                    )
                }
            }
            Spacer(modifier = modifier.width(8.dp))

            // Type Filter
            FinanceDropdownMenu(
                modifier = modifier.weight(1f),
                expanded = viewModel.isTypeMenuOpen,
                onExpandedChange = { viewModel.isTypeMenuOpen = it },
                options = TransactionType.entries,
                onOptionSelected = { type -> viewModel.onTypeSelected(type) },
                itemLabel = { type -> stringResource(viewModel.getTypeResId(type)) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = viewModel.getTypeResId(viewModel.selectedType),
                        icon = R.drawable.type,
                        onClick = { viewModel.isTypeMenuOpen = true }
                    )
                }
            )
        }

        Spacer(modifier = modifier.height(32.dp))

        // Transaction List
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    navController = navController
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_record_found),
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val context = LocalContext.current

    Card(
        onClick = {
            navController.navigate("Detail_Screen/${transaction.id}")
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF404349))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Icon(
                    painter = painterResource(transaction.category.toIconResId()),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = modifier.padding(8.dp)
                )
            }

            Spacer(modifier = modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(transaction.category.toResId()),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = transaction.date.formatRelativeDate(context),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = modifier.weight(1f))

            val amountColor = if (transaction.transaction == TransactionType.INCOME)
                Color.Green else Color.Red

            Text(
                text = transaction.amount.formatAsCurrency(),
                color = amountColor
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
package com.ahmetkaragunlu.financeai.screens.main.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditButton
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.utils.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.utils.formatAsCurrency
import com.ahmetkaragunlu.financeai.utils.toIconResId
import com.ahmetkaragunlu.financeai.utils.toResId
import com.ahmetkaragunlu.financeai.viewmodel.TransactionHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TransactionHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        Row(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Date
            FinanceDropdownMenu(
                modifier = modifier.weight(1f),
                expanded = viewModel.isDateMenuOpen,
                onExpandedChange = { viewModel.isDateMenuOpen = it },
                options = viewModel.dateOptions,
                onOptionSelected = { id -> viewModel.onDateOptionSelected(id) },
                itemLabel = { id -> stringResource(id) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = viewModel.selectedDateResIdUI,
                        icon = R.drawable.calendar,
                        onClick = { viewModel.isDateMenuOpen = true }
                    )
                }
            )
            Spacer(modifier = modifier.width(8.dp))

            // Category
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
                            label = viewModel.selectedCategoryResIdUI,
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

            // Type
            FinanceDropdownMenu(
                modifier = modifier.weight(1f),
                expanded = viewModel.isTypeMenuOpen,
                onExpandedChange = { viewModel.isTypeMenuOpen = it },
                options = viewModel.typeOptions,
                onOptionSelected = { type -> viewModel.onTypeSelected(type) },
                itemLabel = { type -> stringResource(viewModel.getTypeLabel(type)) },
                trigger = {
                    EditButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = viewModel.getTypeLabel(viewModel.selectedTypeUI),
                        icon = R.drawable.type,
                        onClick = { viewModel.isTypeMenuOpen = true }
                    )
                }
            )
        }
        Spacer(modifier = modifier.height(32.dp))
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { transaction ->
                Card(
                    modifier = modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF404349))
                ) {
                    Row(
                        modifier = modifier.fillMaxWidth().padding(16.dp),
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
                                text = formatRelativeDate(transaction.date),
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = modifier.weight(1f))
                        val amountColor = if(transaction.transaction == TransactionType.INCOME) Color.Green else Color.Red
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
            if(transactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_record_found),
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}






@Composable
fun formatRelativeDate(timestamp: Long): String {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullDateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    fun getMidnight(date: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val now = System.currentTimeMillis()
    val todayMidnight = getMidnight(now)
    val yesterdayMidnight = getMidnight(now - TimeUnit.DAYS.toMillis(1))

    val timePart = timeFormatter.format(timestamp)

    return when {
        timestamp >= todayMidnight -> {
            context.getString(R.string.today) + ", " + timePart
        }
        timestamp >= yesterdayMidnight -> {
            context.getString(R.string.yesterday) + ", " + timePart
        }
        else -> {
            fullDateFormatter.format(timestamp)
        }
    }
}
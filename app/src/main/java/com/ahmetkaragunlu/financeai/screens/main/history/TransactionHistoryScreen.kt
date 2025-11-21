package com.ahmetkaragunlu.financeai.screens.main.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditButton
import com.ahmetkaragunlu.financeai.utils.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.utils.toResId
import com.ahmetkaragunlu.financeai.viewmodel.TransactionHistoryViewModel

@Composable
fun TransactionHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
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
                        label = viewModel.selectedDateResId,
                        icon = R.drawable.calendar,
                        onClick = { viewModel.isDateMenuOpen = true }
                    )
                }
            )
            Spacer(modifier = modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                FinanceDropdownMenu(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = viewModel.isCategoryMenuOpen,
                    onExpandedChange = {
                        if (!it) viewModel.isCategoryMenuOpen = false
                    },
                    options = viewModel.categoryOptions, // Dinamik Liste
                    onOptionSelected = { category -> viewModel.onCategorySelected(category) },
                    itemLabel = {category -> stringResource(category.toResId())  },
                    trigger = {
                        EditButton(
                            modifier = Modifier.fillMaxWidth(),
                            label = viewModel.selectedCategoryResId,
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
                        modifier =modifier.padding(top = 4.dp, start = 2.dp)
                    )
                }
            }
            Spacer(modifier = modifier.width(8.dp))
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
                        label = viewModel.getTypeLabel(viewModel.selectedType),
                        icon = R.drawable.type,
                        onClick = { viewModel.isTypeMenuOpen = true }
                    )
                }
            )
        }
        Spacer(modifier = modifier.height(32.dp))
        LazyColumn(modifier = modifier.fillMaxWidth().padding(8.dp)) {
            item {
                Card(
                    modifier = modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF404349))
                ) {
                    Row(
                        modifier = modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = Color.Transparent){
                                Icon(
                                    painter = painterResource(R.drawable.other),
                                    contentDescription = null,
                                    tint =Color.Unspecified
                                )

                        }
                        Spacer(modifier = modifier.width(16.dp))
                        Column {
                            Text("Maas", color = Color.White, fontSize = 16.sp)
                            Text("Bugün,09.30",color = Color.Gray,fontSize = 12.sp)
                        }
                        Spacer(modifier = modifier.weight(1f))
                        Text("20.000 $", color = Color.Green)
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
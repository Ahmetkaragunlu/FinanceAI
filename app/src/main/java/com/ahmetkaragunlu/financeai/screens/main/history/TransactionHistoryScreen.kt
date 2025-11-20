package com.ahmetkaragunlu.financeai.screens.main.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditButton
import com.ahmetkaragunlu.financeai.utils.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.viewmodel.TransactionHistoryViewModel

@Composable
fun TransactionHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colorResource(R.color.background))
                .verticalScroll(rememberScrollState()),
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
            modifier =
                modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
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
            EditButton(
                label = R.string.category,
                icon = R.drawable.categories,
                modifier = modifier.weight(1f),
                onClick = {}
            )
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

    }
}
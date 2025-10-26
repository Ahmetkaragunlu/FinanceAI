package com.ahmetkaragunlu.financeai.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.components.EditTextField
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.ui.theme.AddTransactionScreenTextFieldStyles
import com.ahmetkaragunlu.financeai.util.getCurrencySymbol
import com.ahmetkaragunlu.financeai.viewmodel.AddTransactionViewModel

@Composable
fun AddTransaction(
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.background))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = modifier
                .padding(16.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.updateTransactionType(TransactionType.EXPENSE)
                },
                modifier = modifier.weight(1f),
                colors = if(viewModel.selectedTransactionType == TransactionType.EXPENSE) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(
                    text = stringResource(R.string.expense),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = modifier.width(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.updateTransactionType(TransactionType.INCOME)
                },
                modifier = modifier.weight(1f),
                colors = if(viewModel.selectedTransactionType == TransactionType.INCOME) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF404349))
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(
                    text = stringResource(R.string.income),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

        }
        Spacer(modifier = modifier.height(16.dp))

        Column(
            modifier = modifier.padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditTextField(
                value = viewModel.inputAmount,
                onValueChange = { viewModel.updateInputAmount(it) },
                modifier = Modifier
                    .widthIn(max = 450.dp).padding(bottom = 16.dp)
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                placeholder = R.string.enter_amount,
                colors = AddTransactionScreenTextFieldStyles.textFieldColors(),
                trailingIcon = {
                    Text(
                        getCurrencySymbol(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )
            CategoryDropdownMenu(
                selectedCategory = viewModel.selectedCategory,
                availableCategories = viewModel.availableCategories,
                expanded = viewModel.isCategoryDropdownExpanded,
                onCategorySelected = { viewModel.updateCategory(it) },
                onToggleDropdown = { viewModel.toggleDropdown() },
                onDismissDropdown = { viewModel.dismissDropdown() },
                modifier = Modifier
                    .widthIn(max = 450.dp).padding(bottom = 14.dp)
                    .fillMaxWidth()
            )

            EditTextField(
                value = viewModel.inputNote,
                onValueChange = {viewModel.updateInputNote(it)},
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                placeholder = R.string.enter_your_note,
                modifier = modifier.widthIn(max = 450.dp).fillMaxWidth(),
                colors = AddTransactionScreenTextFieldStyles.textFieldColors()
            )

        }
    }
}

















@Composable
fun CategoryDropdownMenu(
    selectedCategory: CategoryType?,
    availableCategories: List<CategoryType>,
    expanded: Boolean,
    onCategorySelected: (CategoryType) -> Unit,
    onToggleDropdown: () -> Unit,
    onDismissDropdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedCategory?.name?.replace("_", " ") ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.select_category),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clickable { onToggleDropdown() }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleDropdown() },
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF404349),
                disabledTextColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = false,
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissDropdown,
            modifier = Modifier
                .background(colorResource(R.color.background))
        ) {
            availableCategories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.name.replace("_", " "),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        onDismissDropdown()
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}
package com.ahmetkaragunlu.financeai.screens.main.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.BudgetType
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roomdb.type.TransactionType
import com.ahmetkaragunlu.financeai.utils.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.utils.toResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetBottomSheet(
    formState: BudgetFormState,
    isGeneralBudgetSet: Boolean,
    onEvent: (BudgetEvent) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseCategories =
        remember { CategoryType.entries.filter { it.type == TransactionType.EXPENSE } }
    val isEditing = formState.editingId != 0
    val isGeneralBudget = formState.selectedType == BudgetType.GENERAL_MONTHLY

    val primaryColor = Color(0xFF36a2cc)
    val containerColor = Color(0xFF2A2D35)
    val selectedColor = Color(0xFF404349)

    ModalBottomSheet(
        onDismissRequest = { onEvent(BudgetEvent.OnDismissBottomSheet) },
        sheetState = sheetState,
        containerColor = colorResource(R.color.background),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditing) stringResource(R.string.edit_budget_title) else stringResource(
                    R.string.add_new_limit
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (!isGeneralBudget) {
                BudgetTypeSelector(
                    selectedType = formState.selectedType,
                    onTypeSelected = { onEvent(BudgetEvent.OnTypeChange(it)) },
                    containerColor = containerColor,
                    selectedColor = selectedColor,
                    isPercentageEnabled = isGeneralBudgetSet
                )
                Spacer(modifier = Modifier.height(24.dp))
                CategorySelector(
                    selectedCategory = formState.selectedCategory,
                    categories = expenseCategories,
                    onCategorySelected = { onEvent(BudgetEvent.OnCategoryChange(it)) },
                    errorResId = formState.categoryErrorResId
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (formState.selectedType == BudgetType.CATEGORY_PERCENTAGE) {
                FinanceInputTextField(
                    value = formState.percentageInput,
                    onValueChange = {
                        if (it.length <= 3) onEvent(BudgetEvent.OnPercentageChange(it.filter { c -> c.isDigit() }))
                    },
                    label = stringResource(R.string.percentage_label),
                    suffix = stringResource(R.string.percent_symbol),
                    borderColor = primaryColor,
                    errorResId = formState.amountErrorResId
                )
            } else {
                FinanceInputTextField(
                    value = formState.amountInput,
                    onValueChange = { onEvent(BudgetEvent.OnAmountChange(it)) },
                    label = stringResource(R.string.amount_label),
                    suffix = stringResource(R.string.currency_symbol),
                    borderColor = primaryColor,
                    errorResId = formState.amountErrorResId
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onEvent(BudgetEvent.OnSaveClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FinanceInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    borderColor: Color,
    errorResId: Int? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = { Text(suffix) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            isError = errorResId != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            )
        )
        if (errorResId != null) {
            Text(
                text = stringResource(errorResId),
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun BudgetTypeSelector(
    selectedType: BudgetType,
    onTypeSelected: (BudgetType) -> Unit,
    containerColor: Color,
    selectedColor: Color,
    isPercentageEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val types = listOf(
            Pair(BudgetType.CATEGORY_AMOUNT, stringResource(R.string.budget_type_category)),
            Pair(BudgetType.CATEGORY_PERCENTAGE, stringResource(R.string.budget_type_percentage))
        )

        types.forEach { (type, label) ->
            val isSelected = selectedType == type
            val isEnabled = type != BudgetType.CATEGORY_PERCENTAGE || isPercentageEnabled

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) selectedColor else Color.Transparent)
                    .alpha(if (isEnabled) 1f else 0.5f)
                    .clickable(enabled = isEnabled) { onTypeSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategory: CategoryType?,
    categories: List<CategoryType>,
    onCategorySelected: (CategoryType) -> Unit,
    errorResId: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.select_category_label),
            color = Color.Gray,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FinanceDropdownMenu(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            options = categories,
            onOptionSelected = onCategorySelected,
            itemLabel = { stringResource(it.toResId()) },
            trigger = {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.let { stringResource(it.toResId()) }
                            ?: stringResource(R.string.choose_placeholder),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = errorResId != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = if (selectedCategory == null) Color.Gray else Color.White,
                            disabledBorderColor = if (errorResId != null) Color.Red else Color.Gray,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
                            errorBorderColor = Color.Red,
                        )
                    )
                }
            }
        )
        if (errorResId != null) {
            Text(
                text = stringResource(errorResId),
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}
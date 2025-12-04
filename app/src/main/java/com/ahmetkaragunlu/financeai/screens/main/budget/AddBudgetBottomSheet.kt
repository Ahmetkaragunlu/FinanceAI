package com.ahmetkaragunlu.financeai.screens.main.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.ahmetkaragunlu.financeai.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetBottomSheet(
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing = viewModel.editingBudgetId != 0
    val isGeneralBudget = viewModel.selectedType == BudgetType.GENERAL_MONTHLY

    val expenseCategories = remember {
        CategoryType.entries.filter { it.type == TransactionType.EXPENSE }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorResource(R.color.background),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditing) "Bütçeyi Düzenle" else stringResource(R.string.add_new_limit),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isGeneralBudget) {
                BudgetTypeSelector(
                    selectedType = viewModel.selectedType,
                    isLocked = false,
                    onTypeSelected = viewModel::updateSelectedType
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (viewModel.selectedType != BudgetType.GENERAL_MONTHLY) {
                CategorySelector(
                    selectedCategory = viewModel.selectedCategory,
                    categories = expenseCategories,
                    onCategorySelected = viewModel::updateSelectedCategory
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (viewModel.selectedType == BudgetType.CATEGORY_PERCENTAGE) {
                PercentageInput(
                    value = viewModel.inputPercentage,
                    onValueChange = {
                        if (it.length <= 3) viewModel.updateInputPercentage(it.filter { char -> char.isDigit() })
                    }
                )
            } else {
                AmountInput(
                    value = viewModel.inputAmount,
                    onValueChange = viewModel::updateInputAmount
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SaveButton(onClick = viewModel::addBudgetRule)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BudgetTypeSelector(
    selectedType: BudgetType,
    isLocked: Boolean,
    onTypeSelected: (BudgetType) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.alpha(if (isLocked) 0.5f else 1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(Color(0xFF2A2D35), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val types = listOf(
                Triple(BudgetType.CATEGORY_AMOUNT, "Kategori", "🏷️"),
                Triple(BudgetType.CATEGORY_PERCENTAGE, "Yüzde %", "📊")
            )
            types.forEach { (type, label, _) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF404349) else Color.Transparent)
                        .clickable { if (!isLocked) onTypeSelected(type) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    selectedCategory: CategoryType?,
    categories: List<CategoryType>,
    onCategorySelected: (CategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Kategori Seçin",
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
                OutlinedTextField(
                    value = selectedCategory?.let { stringResource(it.toResId()) } ?: "Seçiniz...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = if (selectedCategory == null) Color.Gray else Color.White,
                        focusedBorderColor = Color(0xFF36a2cc),
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        )
    }
}

@Composable
private fun PercentageInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Yüzde (%)") },
        suffix = { Text("%") },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF36a2cc),
            unfocusedBorderColor = Color.Gray
        )
    )
}

@Composable
private fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Tutar (₺)") },
        suffix = { Text("₺") },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF36a2cc),
            unfocusedBorderColor = Color.Gray
        )
    )
}

@Composable
private fun SaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF404349)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = stringResource(R.string.save),
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
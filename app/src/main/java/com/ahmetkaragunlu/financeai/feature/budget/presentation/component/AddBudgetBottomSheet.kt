package com.ahmetkaragunlu.financeai.feature.budget.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.getAccountCurrencySymbol
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetEvent
import com.ahmetkaragunlu.financeai.feature.budget.presentation.BudgetFormState
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form.BudgetInputField
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form.BudgetTypeSelector
import com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form.BudgetCategorySelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetBottomSheet(
    formState: BudgetFormState,
    isGeneralBudgetSet: Boolean,
    onEvent: (BudgetEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseCategories = remember {
        CategoryType.entries.filter { it.type == TransactionType.EXPENSE }
    }
    val isEditing = formState.editingId != 0
    val isGeneralBudget = formState.selectedType == BudgetType.GENERAL_MONTHLY

    val primaryColor = FinanceColors.summaryEnd
    val containerColor = FinanceColors.sheetSurface
    val selectedColor = FinanceColors.elevatedSurface

    ModalBottomSheet(
        onDismissRequest = { onEvent(BudgetEvent.OnDismissBottomSheet) },
        sheetState = sheetState,
        containerColor = colorResource(R.color.background),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    if (isEditing) stringResource(R.string.edit_budget_title)
                    else stringResource(R.string.add_new_limit),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (!isGeneralBudget) {
                BudgetTypeSelector(
                    selectedType = formState.selectedType,
                    onTypeSelected = { onEvent(BudgetEvent.OnTypeChange(it)) },
                    containerColor = containerColor,
                    selectedColor = selectedColor,
                    isPercentageEnabled = isGeneralBudgetSet,
                )
                Spacer(modifier = Modifier.height(24.dp))
                BudgetCategorySelector(
                    selectedCategory = formState.selectedCategory,
                    categories = expenseCategories,
                    onCategorySelected = { onEvent(BudgetEvent.OnCategoryChange(it)) },
                    errorResId = formState.categoryErrorResId,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (formState.selectedType == BudgetType.CATEGORY_PERCENTAGE) {
                BudgetInputField(
                    value = formState.percentageInput,
                    onValueChange = {
                        if (it.length <= 3)
                            onEvent(BudgetEvent.OnPercentageChange(it.filter { c -> c.isDigit() }))
                    },
                    label = stringResource(R.string.percentage_label),
                    suffix = stringResource(R.string.percent_symbol),
                    borderColor = primaryColor,
                    errorResId = formState.amountErrorResId,
                )
            } else {
                BudgetInputField(
                    value = formState.amountInput,
                    onValueChange = { onEvent(BudgetEvent.OnAmountChange(it)) },
                    label = stringResource(R.string.amount_label),
                    suffix = getAccountCurrencySymbol(),
                    borderColor = primaryColor,
                    errorResId = formState.amountErrorResId,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onEvent(BudgetEvent.OnSaveClick) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

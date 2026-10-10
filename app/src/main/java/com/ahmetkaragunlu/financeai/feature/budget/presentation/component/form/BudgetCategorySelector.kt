package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetCategorySelector(
    selectedCategory: CategoryType?,
    categories: List<CategoryType>,
    onCategorySelected: (CategoryType) -> Unit,
    errorResId: Int? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.select_category_label),
            color = FinanceColors.mutedText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        FinanceDropdownMenu(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            options = categories,
            onOptionSelected = onCategorySelected,
            itemLabel = { stringResource(it.toLabelResId()) },
            trigger = {
                Box(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
                    OutlinedTextField(
                        value =
                            selectedCategory?.let { stringResource(it.toLabelResId()) }
                                ?: stringResource(R.string.choose_placeholder),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = errorResId != null,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                disabledTextColor =
                                    if (selectedCategory == null) FinanceColors.mutedText
                                    else FinanceColors.onAccent,
                                disabledBorderColor =
                                    if (errorResId != null) FinanceColors.expense
                                    else FinanceColors.mutedText,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
                                disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
                                errorBorderColor = FinanceColors.expense,
                            ),
                    )
                }
            },
        )
        if (errorResId != null) {
            Text(
                text = stringResource(errorResId),
                color = FinanceColors.expense,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}

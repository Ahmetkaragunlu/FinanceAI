package com.ahmetkaragunlu.financeai.feature.transaction.presentation.add.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.EditTextField
import com.ahmetkaragunlu.financeai.core.ui.component.FinanceDropdownMenu
import com.ahmetkaragunlu.financeai.core.ui.component.getAccountCurrencySymbol
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.TransactionType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId

@Composable
internal fun TransactionInputFields(
    amount: String,
    note: String,
    category: CategoryType?,
    type: TransactionType,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategoryChanged: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCategoryDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    // Amount Field
    EditTextField(
        value = amount,
        onValueChange = onAmountChanged,
        modifier = modifier.widthIn(max = 450.dp).padding(bottom = 16.dp).fillMaxWidth(),
        keyboardOptions =
            KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number,
            ),
        placeholder = R.string.enter_amount,
        colors = AddTransactionFieldStyles.textFieldColors(),
        trailingIcon = {
            Text(getAccountCurrencySymbol(), color = MaterialTheme.colorScheme.onPrimary)
        },
    )

    // Category Dropdown
    FinanceDropdownMenu(
        modifier = modifier.widthIn(max = 450.dp).padding(bottom = 14.dp).fillMaxWidth(),
        expanded = isCategoryDropdownExpanded,
        onExpandedChange = { isOpen -> isCategoryDropdownExpanded = isOpen },
        options = CategoryType.entries.filter { it.type == type },
        onOptionSelected = onCategoryChanged,
        itemLabel = { category -> stringResource(category.toLabelResId()) },
        trigger = {
            OutlinedTextField(
                value = category?.let { stringResource(it.toLabelResId()) } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.select_category),
                        color = FinanceColors.mutedText,
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier =
                            Modifier.clickable {
                                isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                            },
                    )
                },
                modifier =
                    Modifier.fillMaxWidth().clickable {
                        isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                    },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = FinanceColors.fieldSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
            )
        },
    )
    // Note Field
    EditTextField(
        value = note,
        onValueChange = onNoteChanged,
        keyboardOptions =
            KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
        placeholder = R.string.enter_your_note,
        modifier = modifier.widthIn(max = 450.dp).padding(bottom = 14.dp).fillMaxWidth(),
        colors = AddTransactionFieldStyles.textFieldColors(),
    )
}

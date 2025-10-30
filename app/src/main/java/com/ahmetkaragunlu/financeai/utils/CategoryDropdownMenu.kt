package com.ahmetkaragunlu.financeai.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import kotlin.collections.forEach


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
                    color = Color.Gray
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
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
            modifier = Modifier.background(colorResource(R.color.background))
        ) {
            availableCategories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.name.replace("_", " "),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        onDismissDropdown()
                    }
                )
            }
        }
    }
}
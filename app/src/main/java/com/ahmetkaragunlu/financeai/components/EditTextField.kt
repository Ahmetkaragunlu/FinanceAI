package com.ahmetkaragunlu.financeai.components

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp


@Composable
fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int? = null,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    @StringRes placeholder: Int? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: TextFieldColors? = null,
    @StringRes supportingText: Int? = null,
    modifier: Modifier = Modifier,

) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(text = stringResource(it), style = MaterialTheme.typography.labelLarge) } },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        placeholder = placeholder?.let { { Text(text = stringResource(it), style = MaterialTheme.typography.labelLarge, color = Color.Gray) } },
        modifier = modifier,
        shape = shape,
        colors = colors ?: OutlinedTextFieldDefaults.colors(),
        supportingText = supportingText?.let { { Text(text = stringResource(it), color = MaterialTheme.colorScheme.error) } }
    )
}

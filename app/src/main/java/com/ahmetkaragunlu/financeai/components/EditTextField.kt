package com.ahmetkaragunlu.financeai.components

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun EditTextField(
    modifier: Modifier = Modifier,
    value : String,
    onValueChange : (String) -> Unit,
    @StringRes label : Int,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon : @Composable  (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors : TextFieldColors? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = stringResource(label))},
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = true,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        shape = shape,
        colors = colors ?: OutlinedTextFieldDefaults.colors()
    )
}
package com.ahmetkaragunlu.financeai.feature.auth.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.ahmetkaragunlu.financeai.R

@Composable
fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    Icon(
        imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
        contentDescription =
            stringResource(if (visible) R.string.hide_password else R.string.show_password),
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.clickable(role = Role.Button, onClick = onToggle),
    )
}

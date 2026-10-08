package com.ahmetkaragunlu.financeai.app.navigation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTopBar(
    title: String,
    showBack: Boolean,
    showLogout: Boolean,
    onBackClick: () -> Unit,
    onLogoutClicked: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        actions = {
            if (showLogout) {
                IconButton(onClick = onLogoutClicked) {
                    Icon(
                        painterResource(R.drawable.logout),
                        stringResource(R.string.sign_out_title),
                        tint = Color.Unspecified,
                    )
                }
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.navigate_back),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.background)),
    )
}

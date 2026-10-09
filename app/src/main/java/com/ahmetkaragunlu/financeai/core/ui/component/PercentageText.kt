package com.ahmetkaragunlu.financeai.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.ahmetkaragunlu.financeai.core.format.formatAsPercentage
import java.util.Locale

@Composable
fun Int.formatAsUiPercentage(): String =
    formatAsPercentage(LocalConfiguration.current.locales[0] ?: Locale.getDefault())

@Composable
fun Double.formatAsUiPercentage(): String =
    formatAsPercentage(LocalConfiguration.current.locales[0] ?: Locale.getDefault())

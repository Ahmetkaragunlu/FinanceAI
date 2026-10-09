package com.ahmetkaragunlu.financeai.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.ahmetkaragunlu.financeai.core.format.formatAsCurrency
import com.ahmetkaragunlu.financeai.core.money.UNSPECIFIED_CURRENCY
import java.util.Currency

val LocalAccountCurrency = staticCompositionLocalOf { UNSPECIFIED_CURRENCY }

@Composable
fun Double.formatAsAccountCurrency(): String = formatAsCurrency(LocalAccountCurrency.current)

@Composable
fun getAccountCurrencySymbol(): String = Currency.getInstance(LocalAccountCurrency.current).symbol

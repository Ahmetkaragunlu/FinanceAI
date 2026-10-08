package com.ahmetkaragunlu.financeai.core.ui.theme

import androidx.compose.ui.graphics.Brush

object FinanceGradients {
    val summary = Brush.linearGradient(listOf(FinanceColors.summaryStart, FinanceColors.summaryEnd))
    val authentication =
        Brush.linearGradient(listOf(FinanceColors.authStart, FinanceColors.authEnd))
    val financialCard =
        Brush.horizontalGradient(listOf(FinanceColors.cardStart, FinanceColors.cardEnd))
    val suggestion =
        Brush.linearGradient(
            listOf(
                FinanceColors.suggestionStart,
                FinanceColors.suggestionEnd,
                FinanceColors.suggestionEnd,
            )
        )
}

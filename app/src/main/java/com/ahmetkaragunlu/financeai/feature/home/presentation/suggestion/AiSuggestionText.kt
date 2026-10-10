package com.ahmetkaragunlu.financeai.feature.home.presentation.suggestion

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId

data class AiSuggestionText(val message: String, val prompt: String)

@Composable
fun AiSuggestionState.localizedText(): AiSuggestionText =
    when (this) {
        AiSuggestionState.Analyze -> AiSuggestionText(stringResource(R.string.home_ai_analyze), "")
        AiSuggestionState.Planning ->
            AiSuggestionText(
                stringResource(R.string.home_ai_planning),
                stringResource(R.string.home_ai_planning_prompt),
            )
        AiSuggestionState.Healthy ->
            AiSuggestionText(
                stringResource(R.string.home_ai_healthy),
                stringResource(R.string.home_ai_healthy_prompt),
            )
        is AiSuggestionState.NoBudget ->
            AiSuggestionText(
                stringResource(R.string.home_ai_no_budget),
                stringResource(R.string.home_ai_no_budget_prompt, spent.formatAsAccountCurrency()),
            )
        is AiSuggestionState.GeneralExceeded ->
            AiSuggestionText(
                stringResource(
                    R.string.home_ai_general_exceeded,
                    (spent - limit).formatAsAccountCurrency(),
                ),
                stringResource(
                    R.string.home_ai_general_exceeded_prompt,
                    limit.formatAsAccountCurrency(),
                    spent.formatAsAccountCurrency(),
                    percentage,
                ),
            )
        is AiSuggestionState.GeneralNearLimit ->
            AiSuggestionText(
                stringResource(R.string.home_ai_general_near, percentage),
                stringResource(R.string.home_ai_general_near_prompt, percentage),
            )
        is AiSuggestionState.CategoryExceeded -> {
            val name = stringResource(category.toLabelResId())
            AiSuggestionText(
                stringResource(R.string.home_ai_category_exceeded, name),
                stringResource(
                    R.string.home_ai_category_exceeded_prompt,
                    name,
                    limit.formatAsAccountCurrency(),
                    spent.formatAsAccountCurrency(),
                ),
            )
        }
        is AiSuggestionState.CategoryNearLimit -> {
            val name = stringResource(category.toLabelResId())
            AiSuggestionText(
                stringResource(R.string.home_ai_category_near, name, percentage),
                stringResource(R.string.home_ai_category_near_prompt, name, percentage),
            )
        }
    }

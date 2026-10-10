package com.ahmetkaragunlu.financeai.feature.budget.presentation.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.theme.FinanceColors
import com.ahmetkaragunlu.financeai.feature.budget.domain.model.BudgetType

@Composable
internal fun BudgetTypeSelector(
    selectedType: BudgetType,
    onTypeSelected: (BudgetType) -> Unit,
    containerColor: Color,
    selectedColor: Color,
    isPercentageEnabled: Boolean,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(45.dp)
                .background(containerColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val types =
            listOf(
                Pair(BudgetType.CATEGORY_AMOUNT, stringResource(R.string.budget_type_category)),
                Pair(
                    BudgetType.CATEGORY_PERCENTAGE,
                    stringResource(R.string.budget_type_percentage),
                ),
            )

        types.forEach { (type, label) ->
            val isSelected = selectedType == type
            val isEnabled = type != BudgetType.CATEGORY_PERCENTAGE || isPercentageEnabled

            Box(
                modifier =
                    Modifier.weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) selectedColor else Color.Transparent)
                        .alpha(if (isEnabled) 1f else 0.5f)
                        .clickable(enabled = isEnabled) { onTypeSelected(type) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) FinanceColors.onAccent else FinanceColors.mutedText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

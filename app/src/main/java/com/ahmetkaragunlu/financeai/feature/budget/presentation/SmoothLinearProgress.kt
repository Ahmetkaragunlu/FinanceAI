package com.ahmetkaragunlu.financeai.feature.budget.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics

@Composable
fun SmoothLinearProgress(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val coercedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress, 0f..1f) }
                .clip(RoundedCornerShape(50))
                .background(trackColor)
    ) {
        if (coercedProgress > 0f) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(coercedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(color)
            )
        }
    }
}

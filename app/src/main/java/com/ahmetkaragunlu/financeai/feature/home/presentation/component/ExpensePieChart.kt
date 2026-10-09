package com.ahmetkaragunlu.financeai.feature.home.presentation.component

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.R
import com.ahmetkaragunlu.financeai.core.ui.component.formatAsAccountCurrency
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryExpense
import com.ahmetkaragunlu.financeai.feature.transaction.domain.model.CategoryType
import com.ahmetkaragunlu.financeai.feature.transaction.localization.toLabelResId
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpensePieChart(categoryExpenses: List<CategoryExpense>, modifier: Modifier = Modifier) {
    val total = remember(categoryExpenses) { categoryExpenses.sumOf { it.totalAmount } }
    val context = LocalContext.current

    val displayData =
        categoryExpenses.ifEmpty {
            listOf(
                CategoryExpense(CategoryType.FOOD, 0.0),
                CategoryExpense(CategoryType.TRANSPORT, 0.0),
                CategoryExpense(CategoryType.GROCERIES, 0.0),
                CategoryExpense(CategoryType.ENTERTAINMENT, 0.0),
            )
        }

    val displayTotal = if (total <= 0) 4.0 else total

    val categoryData =
        remember(displayData) {
            displayData.mapIndexed { index, expense ->
                val categoryType = expense.category ?: CategoryType.OTHER
                Triple(categoryType, expense.totalAmount, getCategoryColor(index))
            }
        }

    val localeKey = LocalConfiguration.current.locales.toLanguageTags()
    val categoryDisplayStrings =
        remember(categoryData, localeKey) {
            categoryData.associate { (categoryType, _, _) ->
                categoryType to context.getString(categoryType.toLabelResId())
            }
        }
    val description =
        categoryData
            .map { (category, amount, _) ->
                stringResource(
                    R.string.chart_category_value,
                    categoryDisplayStrings[category].orEmpty(),
                    amount.formatAsAccountCurrency(),
                )
            }
            .joinToString("; ")
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { contentDescription = description }
                .fillMaxWidth()
                .height(220.dp)
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val strokeWidth = 35f
            val radius = (canvasSize / 2.8f)
            val centerX = size.width / 2
            val centerY = size.height / 2

            var startAngle = -90f

            displayData.forEachIndexed { index, expense ->
                val sweepAngle =
                    if (total <= 0) {
                        90f
                    } else {
                        (expense.totalAmount / displayTotal * 360f).toFloat()
                    }

                drawArc(
                    color = categoryData[index].third,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )

                val middleAngle =
                    if (displayData.size == 1) {
                        -45f
                    } else {
                        startAngle + (sweepAngle / 2)
                    }
                val angleInRadians = Math.toRadians(middleAngle.toDouble())

                val labelDistance = radius + strokeWidth + 45f
                val labelX = centerX + (labelDistance * cos(angleInRadians)).toFloat()
                val labelY = centerY + (labelDistance * sin(angleInRadians)).toFloat()

                val categoryEnum = categoryData[index].first
                val categoryName = categoryDisplayStrings[categoryEnum] ?: categoryEnum.name

                drawContext.canvas.nativeCanvas.apply {
                    val paint = labelPaint

                    val isLeftSide = labelX < centerX
                    val categoryColor = categoryData[index].third

                    val squareSize = 12f
                    val squareLeft = if (isLeftSide) labelX + 30f else labelX - 30f
                    val squareTop = labelY - 8f

                    paint.color =
                        AndroidColor.argb(
                            (categoryColor.alpha * 255).toInt(),
                            (categoryColor.red * 255).toInt(),
                            (categoryColor.green * 255).toInt(),
                            (categoryColor.blue * 255).toInt(),
                        )

                    drawRect(
                        squareLeft,
                        squareTop,
                        squareLeft + squareSize,
                        squareTop + squareSize,
                        paint,
                    )

                    paint.color = AndroidColor.WHITE
                    paint.textSize = 36f
                    paint.isFakeBoldText = false

                    if (isLeftSide) {
                        paint.textAlign = Paint.Align.RIGHT
                        drawText(categoryName, squareLeft - 8f, labelY + 6f, paint)
                    } else {
                        paint.textAlign = Paint.Align.LEFT
                        drawText(categoryName, squareLeft + squareSize + 8f, labelY + 6f, paint)
                    }
                }

                startAngle += sweepAngle
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total.formatAsAccountCurrency(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private val categoryColors =
    listOf(
        Color(0xFF4DD0E1),
        Color(0xFFFFB74D),
        Color(0xFF9575CD),
        Color(0xFFE91E63),
        Color(0xFF66BB6A),
        Color(0xFFF06292),
        Color(0xFF4FC3F7),
        Color(0xFFFFD54F),
        Color(0xFFEF5350),
        Color(0xFF26C6DA),
        Color(0xFFAB47BC),
        Color(0xFF7E57C2),
        Color(0xFFFF7043),
        Color(0xFF5C6BC0),
    )

private fun getCategoryColor(index: Int): Color = categoryColors[index % categoryColors.size]

package com.ahmetkaragunlu.financeai.utils


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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpensePieChart(
    categoryExpenses: List<CategoryExpense>,
    modifier: Modifier = Modifier
) {
    val total = categoryExpenses.sumOf { it.totalAmount }
    val context = LocalContext.current

    val displayData = categoryExpenses.ifEmpty {
        listOf(
            CategoryExpense(CategoryType.FOOD.name, 0.0),
            CategoryExpense(CategoryType.TRANSPORT.name, 0.0),
            CategoryExpense(CategoryType.GROCERIES.name, 0.0),
            CategoryExpense(CategoryType.ENTERTAINMENT.name, 0.0)
        )
    }

    val displayTotal = if (total <= 0) 4.0 else total

    val categoryData = remember(displayData) {
        displayData.mapIndexed { index, expense ->
            val categoryType = try {
                CategoryType.valueOf(expense.category)
            } catch (e: Exception) {
                CategoryType.OTHER
            }
            Triple(categoryType, expense.totalAmount, getCategoryColor(index))
        }
    }

    val categoryDisplayStrings = remember(categoryData) {
        categoryData.associate { (categoryType, _, _) ->
            categoryType to context.getString(categoryType.toResId())
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasSize = size.minDimension
            val strokeWidth = 35f
            val radius = (canvasSize / 2.8f)
            val centerX = size.width / 2
            val centerY = size.height / 2

            var startAngle = -90f

            displayData.forEachIndexed { index, expense ->
                val sweepAngle = if (total <= 0) {
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
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                val middleAngle = if (displayData.size == 1) {
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
                    val paint = Paint().apply {
                        isAntiAlias = true
                    }

                    val isLeftSide = labelX < centerX
                    val categoryColor = categoryData[index].third

                    val squareSize = 12f
                    val squareLeft = if (isLeftSide) labelX + 30f else labelX - 30f
                    val squareTop = labelY - 8f

                    paint.color = android.graphics.Color.argb(
                        (categoryColor.alpha * 255).toInt(),
                        (categoryColor.red * 255).toInt(),
                        (categoryColor.green * 255).toInt(),
                        (categoryColor.blue * 255).toInt()
                    )

                    drawRect(
                        squareLeft,
                        squareTop,
                        squareLeft + squareSize,
                        squareTop + squareSize,
                        paint
                    )

                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 36f
                    paint.isFakeBoldText = false

                    if (isLeftSide) {
                        paint.textAlign = Paint.Align.RIGHT
                        drawText(
                            categoryName,
                            squareLeft - 8f,
                            labelY + 6f,
                            paint
                        )
                    } else {
                        paint.textAlign = Paint.Align.LEFT
                        drawText(
                            categoryName,
                            squareLeft + squareSize + 8f,
                            labelY + 6f,
                            paint
                        )
                    }
                }

                startAngle += sweepAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = total.formatAsCurrency(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private fun getCategoryColor(index: Int): Color {
    val colors = listOf(
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
    return colors[index % colors.size]
}
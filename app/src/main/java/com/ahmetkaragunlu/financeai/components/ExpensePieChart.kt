package com.ahmetkaragunlu.financeai.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmetkaragunlu.financeai.roomdb.type.CategoryType
import com.ahmetkaragunlu.financeai.roommodel.CategoryExpense
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpensePieChart(
    categoryExpenses: List<CategoryExpense>,
    modifier: Modifier = Modifier
) {
    val total = categoryExpenses.sumOf { it.totalAmount }

    val displayData = if (categoryExpenses.isEmpty()) {
        listOf(
            CategoryExpense(CategoryType.FOOD.name, 0.0),
            CategoryExpense(CategoryType.TRANSPORT.name, 0.0),
            CategoryExpense(CategoryType.GROCERIES.name, 0.0),
            CategoryExpense(CategoryType.ENTERTAINMENT.name, 0.0)
        )
    } else {
        categoryExpenses
    }

    val displayTotal = if (total <= 0) 4.0 else total

    // Her kategori için renk ata
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

                val middleAngle = startAngle + (sweepAngle / 2)
                val angleInRadians = Math.toRadians(middleAngle.toDouble())

                val labelDistance = radius + strokeWidth + 45f
                val labelX = centerX + (labelDistance * cos(angleInRadians)).toFloat()
                val labelY = centerY + (labelDistance * sin(angleInRadians)).toFloat()

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                    }

                    val isLeftSide = labelX < centerX

                    // Renkli kare arka plan
                    val squareSize = 12f
                    val squareLeft = if (isLeftSide) labelX + 30f else labelX - 30f
                    val squareTop = labelY - 8f
                    paint.color = categoryData[index].third.hashCode()
                    drawRect(
                        squareLeft,
                        squareTop,
                        squareLeft + squareSize,
                        squareTop + squareSize,
                        paint
                    )

                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 28f
                    paint.isFakeBoldText = false

                    if (isLeftSide) {
                        paint.textAlign = android.graphics.Paint.Align.RIGHT
                        drawText(
                            getCategoryDisplayName(categoryData[index].first),
                            squareLeft - 8f,
                            labelY + 6f,
                            paint
                        )
                    } else {
                        paint.textAlign = android.graphics.Paint.Align.LEFT
                        drawText(
                            getCategoryDisplayName(categoryData[index].first),
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
                text = formatCurrency(total),
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
private fun getCategoryDisplayName(category: CategoryType): String {
    return when (category) {
        CategoryType.FOOD -> CategoryType.FOOD.name
        CategoryType.GROCERIES -> CategoryType.GROCERIES.name
        CategoryType.COFFEE_TEA -> CategoryType.COFFEE_TEA.name
        CategoryType.DESSERT_SWEETS -> CategoryType.DESSERT_SWEETS.name
        CategoryType.TRANSPORT -> CategoryType.TRANSPORT.name
        CategoryType.RENT -> CategoryType.RENT.name
        CategoryType.ENTERTAINMENT -> CategoryType.ENTERTAINMENT.name
        CategoryType.HEALTH -> CategoryType.HEALTH.name
        CategoryType.BILLS -> CategoryType.BILLS.name
        CategoryType.CLOTHING -> CategoryType.CLOTHING.name
        CategoryType.EDUCATION -> CategoryType.EDUCATION.name
        CategoryType.HOME_DECORATION -> CategoryType.HOME_DECORATION.name
        CategoryType.GIFTS_DONATION -> CategoryType.GIFTS_DONATION.name
        CategoryType.OTHER -> CategoryType.OTHER.name
        else -> CategoryType.OTHER.name
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    return formatter.format(amount)
}
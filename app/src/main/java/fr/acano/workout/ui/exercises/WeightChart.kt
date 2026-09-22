package fr.acano.workout.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Courbe d'évolution de la charge. Dessinée à la main : une soixantaine de lignes,
 * contre une bibliothèque de graphiques à embarquer et à maintenir pour un seul écran.
 */
@Composable
fun WeightChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline

    Canvas(modifier) {
        val minValue = points.min()
        val maxValue = points.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        val paddingY = size.height * 0.12f
        val usableHeight = size.height - paddingY * 2
        val stepX = size.width / (points.size - 1)

        fun yFor(value: Double) =
            paddingY + (1f - ((value - minValue) / span).toFloat()) * usableHeight

        // Trois repères horizontaux discrets : min, milieu, max.
        listOf(minValue, (minValue + maxValue) / 2, maxValue).forEach { value ->
            val y = yFor(value)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        val path = Path().apply {
            points.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yFor(value)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round),
        )

        points.forEachIndexed { index, value ->
            drawCircle(
                color = lineColor,
                radius = 7f,
                center = Offset(index * stepX, yFor(value)),
            )
        }
    }
}

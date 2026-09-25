package fr.acano.workout.ui.exercises

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatDate
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.theme.WorkoutMotion

/**
 * Courbe d'évolution de la charge, une valeur par séance.
 *
 * Dessinée à la main : une bibliothèque de graphiques coûterait une dépendance
 * et un style à réaligner sur le thème, pour un seul écran. Le tracé se déroule
 * à l'ouverture, ce qui suffit à faire lire la courbe de gauche à droite.
 *
 * Pour qu'elle se lise et pas seulement se regarde : la charge la plus haute et
 * la plus basse sont écrites face à leurs repères, les dates de la première et
 * de la dernière séance sous l'axe, et le record est marqué d'un anneau.
 */
@Composable
fun WeightChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    val unit = LocalWeightUnit.current
    val values = points.map { it.second }
    val labelStyle = MaterialTheme.typography.labelMedium
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier) {
        Row(Modifier.weight(1f)) {
            // Les deux repères extrêmes, calés sur les pointillés (marge verticale de 14 %).
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .padding(vertical = 4.dp),
            ) {
                Text(formatWeight(values.max(), unit), style = labelStyle, color = labelColor)
                Text(formatWeight(values.min(), unit), style = labelStyle, color = labelColor)
            }
            ChartCanvas(values, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().padding(start = 56.dp, top = 4.dp)) {
            Text(formatDate(points.first().first), style = labelStyle, color = labelColor, modifier = Modifier.weight(1f))
            Text(formatDate(points.last().first), style = labelStyle, color = labelColor)
        }
    }
}

@Composable
private fun ChartCanvas(points: List<Double>, modifier: Modifier) {

    var started by remember(points) { mutableStateOf(false) }
    val reveal by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = WorkoutMotion.slowSpatial(),
        label = "chartReveal",
    )
    LaunchedEffect(points) { started = true }

    val lineColor = MaterialTheme.colorScheme.primary
    val recordColor = MaterialTheme.colorScheme.tertiary
    val recordIndex = points.indexOf(points.max())
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val description = "Évolution de ${formatWeight(points.first(), LocalWeightUnit.current)} à ${formatWeight(points.last(), LocalWeightUnit.current)}"

    Canvas(modifier.semantics { contentDescription = description }) {
        val minValue = points.min()
        val maxValue = points.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        val paddingY = size.height * 0.14f
        val usableHeight = size.height - paddingY * 2
        val stepX = size.width / (points.size - 1)

        fun yFor(value: Double) =
            paddingY + (1f - ((value - minValue) / span).toFloat()) * usableHeight

        // Repères discrets : la charge minimale, la médiane, la maximale.
        val dashes = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
        listOf(minValue, (minValue + maxValue) / 2, maxValue).forEach { value ->
            val y = yFor(value)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f,
                pathEffect = dashes,
            )
        }

        val visibleCount = (points.size * reveal).coerceAtLeast(1f)
        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, value ->
            if (index > visibleCount) return@forEachIndexed
            val x = index * stepX
            val y = yFor(value)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        val lastVisibleX = (visibleCount.toInt().coerceAtMost(points.lastIndex)) * stepX
        fillPath.lineTo(lastVisibleX, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)),
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        points.forEachIndexed { index, value ->
            if (index > visibleCount) return@forEachIndexed
            val center = Offset(index * stepX, yFor(value))
            drawCircle(
                color = lineColor,
                radius = if (index == points.lastIndex) 10f else 6f,
                center = center,
            )
            // Le record : un anneau, qui reste lisible même s'il s'agit du dernier point.
            if (index == recordIndex) {
                drawCircle(color = recordColor, radius = 18f, center = center, style = Stroke(width = 4f))
            }
        }
    }
}

package fr.acano.workout.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.domain.SessionComparison
import fr.acano.workout.domain.WeightUnit
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatKcal
import fr.acano.workout.ui.common.formatSet
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.formatWeightValue
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.TrendIndicator
import fr.acano.workout.ui.theme.WorkoutTheme
import kotlin.math.abs

/**
 * Un exercice d'une séance passée.
 *
 * En tête, l'exercice et ce qui a changé depuis la dernière fois ; dessous, les
 * séries en tuiles, de gauche à droite dans l'ordre où elles ont été faites :
 * la charge en grand, les répétitions en petit. L'ordre suffit à situer une
 * série, sans étiquette. La meilleure série est teintée — c'est l'information
 * qu'on cherche en relisant une séance, pas une décoration.
 */
@Composable
fun ExerciseSetsBreakdown(line: SessionDetailLine, modifier: Modifier = Modifier) {
    val unit = LocalWeightUnit.current
    Column(modifier.fillMaxWidth().padding(vertical = WorkoutTheme.spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            line.exercise?.let {
                ExerciseImage(
                    exercise = it,
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.width(WorkoutTheme.spacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(line.exerciseName, style = MaterialTheme.typography.titleMedium)
                line.comparison?.let { ComparisonLine(it, unit) }
            }
            line.kcal?.let {
                Spacer(Modifier.width(WorkoutTheme.spacing.sm))
                Text(
                    text = formatKcal(it),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.md))

        SetTiles(line.sets, line.bestSetIndex)
    }
}

/** Les séries d'un exercice en tuiles, dans l'ordre, la meilleure teintée. */
@Composable
fun SetTiles(sets: List<SetResultEntity>, bestSetIndex: Int?, modifier: Modifier = Modifier) {
    val unit = LocalWeightUnit.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm),
        modifier = modifier,
    ) {
        sets.forEachIndexed { index, set ->
            SetTile(set, highlighted = index == bestSetIndex, unit = unit)
        }
    }
}

@Composable
private fun ComparisonLine(comparison: SessionComparison, unit: WeightUnit) {
    val colors = MaterialTheme.colorScheme
    val (text, color, trend) = when (comparison) {
        SessionComparison.FirstTime -> Triple(stringResource(R.string.history_first_time), colors.onSurfaceVariant, 0)
        is SessionComparison.Weight -> when {
            comparison.deltaKg > 0 ->
                Triple(stringResource(R.string.history_weight_up, formatWeight(comparison.deltaKg, unit)), colors.primary, 1)
            comparison.deltaKg < 0 ->
                Triple(stringResource(R.string.history_weight_down, formatWeight(abs(comparison.deltaKg), unit)), colors.onSurfaceVariant, -1)
            else -> Triple(stringResource(R.string.history_same_weight), colors.onSurfaceVariant, 0)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        if (trend != 0) {
            TrendIndicator(trend)
            Spacer(Modifier.width(WorkoutTheme.spacing.xs))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/**
 * Une série : la valeur principale en grand (charge, répétitions ou durée),
 * son complément en petit. Largeur fixe, pour que les tuiles s'alignent en
 * colonnes d'un exercice à l'autre.
 */
@Composable
private fun SetTile(set: SetResultEntity, highlighted: Boolean, unit: WeightUnit) {
    val (value, caption) = tileContent(set, unit)
    val colors = MaterialTheme.colorScheme
    val spoken = stringResource(
        if (highlighted) R.string.history_set_spoken_best else R.string.history_set_spoken,
        set.setNumber,
        formatSet(set.weightKg, set.repetitions, set.durationSeconds, unit),
    )
    Surface(
        color = if (highlighted) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (highlighted) colors.onPrimaryContainer else colors.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .width(TILE_WIDTH)
            .semantics { contentDescription = spoken },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(vertical = WorkoutTheme.spacing.md)
                .clearAndSetSemantics { },
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) colors.onPrimaryContainer else colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Ce que la tuile met en grand, et ce qu'elle écrit dessous. */
private fun tileContent(set: SetResultEntity, unit: WeightUnit): Pair<String, String> {
    val weight = set.weightKg
    val reps = set.repetitions
    val duration = set.durationSeconds
    return when {
        weight != null && reps != null -> formatWeightValue(weight, unit) to "${unit.symbol} × $reps"
        reps != null -> "$reps" to if (reps > 1) "reps" else "rep"
        duration != null -> "%d:%02d".format(duration / 60, duration % 60) to "min"
        weight != null -> formatWeightValue(weight, unit) to unit.symbol
        else -> "—" to ""
    }
}

private val TILE_WIDTH = 76.dp

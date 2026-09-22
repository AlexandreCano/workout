package fr.acano.workout.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.formatShortDay
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.targetLabel
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.history.BackRow
import fr.acano.workout.ui.history.EmptyState
import fr.acano.workout.ui.history.ScreenTitle
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Catalogue des exercices.
 *
 * Chaque ligne répond à la seule question qu'on se pose devant une machine :
 * « j'avais mis combien la dernière fois ? ». La charge est donc l'élément le
 * plus lourd typographiquement de la ligne, pas le nom.
 */
@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel,
    onOpenExercise: (String) -> Unit,
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(
            start = WorkoutTheme.spacing.xl,
            end = WorkoutTheme.spacing.xl,
            top = WorkoutTheme.spacing.xl,
            bottom = WorkoutTheme.spacing.xxxl,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenTitle("Exercices")
            SectionHeader("Dernière charge utilisée")
        }
        items(exercises, key = { it.exercise.id }) { row ->
            ExerciseRow(row, onClick = { onOpenExercise(row.exercise.id) })
            RowDivider()
        }
    }
}

@Composable
private fun ExerciseRow(row: ExerciseRow, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = WorkoutTheme.spacing.md),
    ) {
        ExerciseImage(
            exerciseId = row.exercise.id,
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.width(WorkoutTheme.spacing.lg))

        Column(Modifier.weight(1f)) {
            Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${row.exercise.plannedSets} × ${row.exercise.targetLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatWeight(row.lastWeightKg),
                style = WorkoutTheme.emphasis.metricSmall,
                color = if (row.lastWeightKg != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            row.lastPerformedAt?.let {
                Text(
                    text = formatShortDay(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Détail d'un exercice : l'animation en grand, la charge courante, la courbe,
 * puis l'historique séance par séance.
 */
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercise

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        BackRow(onBack)

        if (exercise != null) {
            ExerciseImage(
                exerciseId = exercise.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                shape = MaterialTheme.shapes.large,
            )

            Spacer(Modifier.height(WorkoutTheme.spacing.xl))

            Text(exercise.name, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(WorkoutTheme.spacing.xs))
            Text(
                text = "${exercise.plannedSets} séries  ·  ${exercise.targetLabel()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val currentWeight = state.history.firstNotNullOfOrNull { it.weightKg }
        if (currentWeight != null) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xl))
            Text(
                text = formatWeight(currentWeight),
                style = WorkoutTheme.emphasis.metric,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Dernière charge",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val weightHistory = state.history
            .mapNotNull { entry -> entry.weightKg }
            .reversed()

        if (weightHistory.size >= 2) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader("Évolution de la charge")
            WeightChart(
                points = weightHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        SectionHeader("Historique")

        if (state.history.isEmpty()) {
            EmptyState(
                title = "Aucune série",
                message = "Cet exercice n'a pas encore été réalisé.",
            )
        }

        state.history.forEachIndexed { index, entry ->
            if (index > 0) RowDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WorkoutTheme.spacing.lg),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = formatShortDay(entry.date),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            entry.repetitions.isNotEmpty() ->
                                entry.repetitions.joinToString("  ·  ")
                            entry.durations.isNotEmpty() ->
                                entry.durations.joinToString("  ·  ") { "${it}s" }
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (entry.weightKg != null) {
                    Text(
                        text = formatWeight(entry.weightKg),
                        style = WorkoutTheme.emphasis.metricSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxxl))
    }
}

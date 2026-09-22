package fr.acano.workout.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.AppCard
import fr.acano.workout.ui.common.ExerciseImage
import fr.acano.workout.ui.common.SectionTitle
import fr.acano.workout.ui.common.formatShortDay
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.targetLabel
import fr.acano.workout.ui.history.EmptyState

@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel,
    onOpenExercise: (String) -> Unit,
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Exercices",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        items(exercises, key = { it.exercise.id }) { row ->
            AppCard(Modifier.clickable { onOpenExercise(row.exercise.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseImage(
                        exerciseId = row.exercise.id,
                        modifier = Modifier.size(64.dp),
                        cornerRadius = 14.dp,
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                    ) {
                        Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${row.exercise.plannedSets} × ${row.exercise.targetLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatWeight(row.lastWeightKg),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        row.lastPerformedAt?.let {
                            Text(
                                formatShortDay(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

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
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour")
            }
            Text(exercise?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
        }

        if (exercise != null) {
            ExerciseImage(
                exerciseId = exercise.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .aspectRatio(16f / 10f),
            )
            Text(
                text = "${exercise.plannedSets} séries  ·  ${exercise.targetLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Spacer(Modifier.height(22.dp))

        val weightHistory = state.history.mapNotNull { entry ->
            entry.weightKg?.let { entry.date to it }
        }.reversed()

        if (weightHistory.size >= 2) {
            SectionTitle("Évolution de la charge")
            Spacer(Modifier.height(10.dp))
            WeightChart(
                points = weightHistory.map { it.second },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
            Spacer(Modifier.height(22.dp))
        }

        SectionTitle("Historique")
        Spacer(Modifier.height(10.dp))

        if (state.history.isEmpty()) {
            EmptyState("Aucune série enregistrée pour cet exercice.")
        }

        state.history.forEach { entry ->
            AppCard(Modifier.padding(bottom = 10.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatShortDay(entry.date),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (entry.weightKg != null) {
                            Text(
                                formatWeight(entry.weightKg),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    val detail = when {
                        entry.repetitions.isNotEmpty() -> entry.repetitions.joinToString("  /  ")
                        entry.durations.isNotEmpty() -> entry.durations.joinToString("  /  ") { "${it}s" }
                        else -> ""
                    }
                    if (detail.isNotEmpty()) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

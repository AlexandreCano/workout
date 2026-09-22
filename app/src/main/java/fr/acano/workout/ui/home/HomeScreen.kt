package fr.acano.workout.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.domain.WorkoutType
import fr.acano.workout.ui.common.AppCard
import fr.acano.workout.ui.common.PrimaryActionButton
import fr.acano.workout.ui.common.SectionTitle
import fr.acano.workout.ui.common.StatTile
import fr.acano.workout.ui.common.formatDay
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.theme.Star
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSession: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun start(type: WorkoutType) {
        scope.launch { onOpenSession(viewModel.startSession(type)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Workout",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "⭐ ${state.stars}",
                style = MaterialTheme.typography.headlineSmall,
                color = Star,
            )
        }

        Text(
            text = when (state.stars) {
                0 -> "Aucune séance pour l'instant"
                1 -> "1 séance réalisée"
                else -> "${state.stars} séances réalisées"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${state.stars}", "Total", Modifier.weight(1f))
            StatTile("${state.upperCount}", "Haut du corps", Modifier.weight(1f))
            StatTile("${state.lowerCount}", "Bas du corps", Modifier.weight(1f))
        }

        state.resumable?.let { resumable ->
            Spacer(Modifier.height(20.dp))
            ResumeCard(resumable, onClick = { onOpenSession(resumable.sessionId) })
        }

        if (state.lastSessionAt != null) {
            Spacer(Modifier.height(20.dp))
            SectionTitle("Dernière séance")
            Text(
                text = "${state.lastSessionType?.label}  ·  ${formatDay(state.lastSessionAt!!)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        PrimaryActionButton(
            text = WorkoutType.UPPER_BODY.label.uppercase(),
            onClick = { start(WorkoutType.UPPER_BODY) },
            enabled = state.resumable == null,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryActionButton(
            text = WorkoutType.LOWER_BODY.label.uppercase(),
            onClick = { start(WorkoutType.LOWER_BODY) },
            enabled = state.resumable == null,
        )

        if (state.progression.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionTitle("Progression récente")
            Spacer(Modifier.height(8.dp))
            state.progression.forEach { line ->
                ProgressionRow(line)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ResumeCard(resumable: ResumableSession, onClick: () -> Unit) {
    AppCard {
        Column {
            SectionTitle("Séance en cours")
            Spacer(Modifier.height(6.dp))
            Text(resumable.type.label, style = MaterialTheme.typography.titleLarge)
            Text(
                text = buildString {
                    append(resumable.exerciseName)
                    if (resumable.plannedSets > 1) {
                        append("  ·  Série ${resumable.setNumber} / ${resumable.plannedSets}")
                    }
                    append("  ·  ${resumable.stepIndex} / ${resumable.totalSteps}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(14.dp))
            PrimaryActionButton(text = "REPRENDRE", onClick = onClick)
        }
    }
}

@Composable
private fun ProgressionRow(line: ProgressionLine) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            line.exerciseName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatWeight(line.weightKg),
            style = MaterialTheme.typography.bodyLarge,
        )
        when (line.trend) {
            1 -> Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = "en progression",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp),
            )
            -1 -> Icon(
                Icons.Rounded.ArrowDownward,
                contentDescription = "en baisse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
            else -> Spacer(Modifier.padding(start = 6.dp))
        }
    }
}

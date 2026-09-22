package fr.acano.workout.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Previews du design system.
 *
 * Elles existent pour vérifier d'un coup d'œil, dans Android Studio, que chaque
 * composant tient dans les deux thèmes — c'est le seul contrôle visuel possible
 * sans appareil. Les composants qui dépendent du contexte applicatif
 * (l'animation d'un exercice) en sont volontairement absents.
 */
@Preview(name = "Composants · sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Composants · clair", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun DesignSystemPreview() {
    WorkoutTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                verticalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.xl),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WorkoutTheme.spacing.xl),
            ) {
                SectionHeader("Progression")
                StepBar(currentStep = 3, totalSteps = 7)

                Text("Chest Press", style = WorkoutTheme.emphasis.exerciseName)
                SetProgress(completedSets = 1, currentSet = 2, totalSets = 4)

                WeightSelector(
                    weightKg = 42.5,
                    stepKg = 2.5,
                    onChange = {},
                    supportingText = "Dernière séance : 40 kg",
                )

                RepsSelector(selected = 11, minReps = 8, maxReps = 12, onSelect = {})

                WorkoutPrimaryButton(
                    text = "VALIDER LA SÉRIE",
                    onClick = {},
                    icon = Icons.Rounded.Check,
                )

                ActionRow {
                    WorkoutTonalButton("Pause", {}, Modifier.weight(1f))
                    WorkoutTonalButton("30 s", {}, Modifier.weight(1f))
                    WorkoutTonalButton("Passer", {}, Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.md)) {
                    StatCard("17", "Total", Modifier.weight(1f), emphasized = true)
                    StatCard("9", "Haut du corps", Modifier.weight(1f))
                    StatCard("8", "Bas du corps", Modifier.weight(1f))
                }

                ListRow(
                    trailing = {
                        Row {
                            Text("45 kg", style = WorkoutTheme.emphasis.metricSmall)
                            TrendIndicator(trend = 1)
                        }
                    },
                ) {
                    Text("Leg Press", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "4 × 8–12 reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RowDivider()
            }
        }
    }
}

@Preview(name = "Chrono · en cours", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TimerRunningPreview() {
    WorkoutTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            WorkoutTimer(
                clock = "00:47",
                progress = 0.78f,
                label = "Récupération",
                finished = false,
                caption = "Prochaine série\nChest Press  ·  Série 3 / 4",
                modifier = Modifier.padding(WorkoutTheme.spacing.xl),
            )
        }
    }
}

@Preview(name = "Chrono · terminé", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TimerFinishedPreview() {
    WorkoutTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            WorkoutTimer(
                clock = "00:00",
                progress = 1f,
                label = "C'est reparti",
                finished = true,
                caption = "Chest Press  ·  Série 3 / 4",
                modifier = Modifier.padding(WorkoutTheme.spacing.xl),
            )
        }
    }
}

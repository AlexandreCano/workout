package fr.acano.workout.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Choix de l'exercice à faire maintenant, quand la machine prévue est occupée.
 *
 * Un appui suffit : l'exercice choisi passe en tête, les autres restent à faire
 * ensuite dans leur ordre. Seuls les exercices restants sont proposés, et celui
 * en cours est présent mais inerte, pour garder le repère de position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderSheet(
    steps: List<PendingStep>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = WorkoutTheme.spacing.xl)) {
            Text("Faire maintenant", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(WorkoutTheme.spacing.xs))
            Text(
                text = "L'exercice choisi passe en premier, les autres suivent.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(WorkoutTheme.spacing.xl))
            SectionHeader("${steps.size} exercices restants")

            steps.forEach { step ->
                StepRow(step = step, onClick = { onSelect(step.exerciseSessionId) })
                Spacer(Modifier.height(WorkoutTheme.spacing.sm))
            }

            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        }
    }
}

@Composable
private fun StepRow(step: PendingStep, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !step.isCurrent,
        shape = MaterialTheme.shapes.medium,
        color = if (step.isCurrent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (step.isCurrent) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = WorkoutTheme.spacing.xl,
                vertical = WorkoutTheme.spacing.lg,
            ),
        ) {
            Column(Modifier.weight(1f)) {
                Text(step.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        step.isCurrent -> "En cours"
                        step.completedSets > 0 ->
                            "Repris à la série ${step.completedSets + 1} / ${step.plannedSets}"
                        step.plannedSets > 1 -> "${step.plannedSets} séries"
                        else -> "1 série"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step.isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!step.isCurrent) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

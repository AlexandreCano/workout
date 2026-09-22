package fr.acano.workout.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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

/**
 * Choix de l'exercice à faire maintenant, quand la machine prévue est occupée.
 *
 * Un appui suffit : l'exercice choisi passe en tête, les autres restent à faire
 * ensuite dans leur ordre. Seuls les exercices restants sont proposés.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderSheet(
    steps: List<PendingStep>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Faire maintenant", style = MaterialTheme.typography.titleLarge)
            Text(
                "L'exercice choisi passe en premier, les autres suivent.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )

            steps.forEach { step ->
                StepRow(step = step, onClick = { onSelect(step.exerciseSessionId) })
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepRow(step: PendingStep, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !step.isCurrent,
        shape = RoundedCornerShape(16.dp),
        color = if (step.isCurrent) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(step.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when {
                        step.isCurrent -> "En cours"
                        step.completedSets > 0 ->
                            "Repris à la série ${step.completedSets + 1} / ${step.plannedSets}"
                        step.plannedSets > 1 -> "${step.plannedSets} séries"
                        else -> "1 série"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!step.isCurrent) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

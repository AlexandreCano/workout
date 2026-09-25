package fr.acano.workout.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import fr.acano.workout.R
import fr.acano.workout.ui.components.ReorderableItem
import fr.acano.workout.ui.components.ReorderableList
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTextButton
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Réorganisation libre des exercices restants.
 *
 * Écran à part entière plutôt que feuille modale : un glisser-déposer vertical
 * à l'intérieur d'une `ModalBottomSheet` entre en conflit avec le geste de la
 * feuille elle-même, et l'un des deux finit toujours par gagner au mauvais moment.
 *
 * L'ordre est modifié localement puis appliqué à la validation. Appliquer chaque
 * permutation immédiatement ferait changer l'exercice courant sous le doigt en
 * plein glissé.
 */
@Composable
fun ReorderStage(
    steps: List<PendingStep>,
    onConfirm: (List<Long>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order: SnapshotStateList<PendingStep> = remember(steps.map { it.exerciseSessionId }) {
        steps.toMutableStateList()
    }
    BackHandler { onCancel() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        Text(
            text = stringResource(R.string.session_reorder_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.xs))
        Text(
            text = stringResource(R.string.session_reorder_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))
        SectionHeader(stringResource(R.string.session_reorder_section))

        ReorderableList(
            items = order.map {
                ReorderableItem(
                    id = it.exerciseSessionId,
                    title = it.name,
                    subtitle = it.progressLabel(),
                )
            },
            onMove = { from, to -> order.add(to, order.removeAt(from)) },
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        WorkoutPrimaryButton(
            text = stringResource(R.string.session_reorder_confirm),
            // Toujours actif : un ordre inchangé est ignoré côté ViewModel, et un
            // gros bouton grisé se lit comme une panne plutôt qu'une contrainte.
            onClick = { onConfirm(order.map { it.exerciseSessionId }) },
            icon = Icons.Rounded.Check,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.sm))

        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            WorkoutTextButton(text = stringResource(R.string.session_cancel), onClick = onCancel)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
    }
}

@Composable
private fun PendingStep.progressLabel(): String = when {
    completedSets > 0 -> stringResource(R.string.session_resumed_at_set, completedSets + 1, plannedSets)
    else -> plannedSets.coerceAtLeast(1).let { pluralStringResource(R.plurals.session_sets_count, it, it) }
}

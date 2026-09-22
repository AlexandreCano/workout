package fr.acano.workout.ui.session

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.targetLabel
import fr.acano.workout.ui.components.ActionRow
import fr.acano.workout.ui.components.ExerciseHeader
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.RepsSelector
import fr.acano.workout.ui.components.WeightSelector
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTextButton
import fr.acano.workout.ui.components.WorkoutTonalButton
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * L'écran de saisie d'une série : le plus utilisé de l'application.
 *
 * L'ordre de lecture est imposé par l'usage, pas par la structure des données :
 * nom de l'exercice, où j'en suis, ce que je vise, à quoi ça ressemble, avec
 * quelle charge, combien j'en ai fait — puis l'action. Chaque bloc est séparé
 * par de l'espace plutôt que par un conteneur, pour garder une seule colonne
 * de lecture.
 */
@Composable
fun SetEntryStage(
    step: CurrentStep,
    canReorder: Boolean,
    canUndo: Boolean,
    onWeightChange: (Double) -> Unit,
    onValidateReps: (Int) -> Unit,
    onStartEffort: () -> Unit,
    onMarkTimedDone: () -> Unit,
    onPostpone: () -> Unit,
    onOpenReorder: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        ExerciseHeader(
            name = step.exercise.name,
            completedSets = step.setsDoneToday.size,
            currentSet = step.setNumber,
            totalSets = step.plannedSets,
            targetLabel = step.exercise.targetLabel(),
        )

        if (canReorder) {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            ActionRow {
                WorkoutTonalButton(
                    text = "Machine occupée",
                    onClick = onPostpone,
                    icon = Icons.Rounded.SwapVert,
                    modifier = Modifier.weight(1f),
                )
                WorkoutTonalButton(
                    text = "Changer",
                    onClick = onOpenReorder,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        ExerciseImage(
            exerciseId = step.exercise.id,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f),
            shape = MaterialTheme.shapes.large,
        )

        if (step.setsDoneToday.isNotEmpty()) {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            CompletedSetsStrip(step)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        when (step.exercise.kind) {
            ExerciseKind.WEIGHTED_REPS -> WeightedRepsControls(step, onWeightChange, onValidateReps)
            ExerciseKind.REPS_ONLY -> RepsOnlyControls(step, onValidateReps)
            ExerciseKind.TIMED -> TimedControls(step, onStartEffort, onMarkTimedDone)
        }

        if (canUndo) {
            Spacer(Modifier.height(WorkoutTheme.spacing.sm))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WorkoutTextButton(
                    text = "Annuler la dernière série",
                    onClick = onUndo,
                    icon = Icons.Rounded.Undo,
                )
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
    }
}

/** Les séries déjà faites aujourd'hui, en rappel discret sous l'animation. */
@Composable
private fun CompletedSetsStrip(step: CurrentStep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Déjà fait",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        step.setsDoneToday.forEach { set ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = set.repetitions?.toString()
                        ?: set.durationSeconds?.let { "${it}s" }
                        ?: "—",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(
                        horizontal = WorkoutTheme.spacing.md,
                        vertical = WorkoutTheme.spacing.xs,
                    ),
                )
            }
        }
    }
}

@Composable
private fun WeightedRepsControls(
    step: CurrentStep,
    onWeightChange: (Double) -> Unit,
    onValidate: (Int) -> Unit,
) {
    val minReps = step.exercise.targetRepsMin ?: 8
    val maxReps = step.exercise.targetRepsMax ?: 12
    var reps by remember(step.exerciseSessionId, step.setNumber) {
        mutableIntStateOf(step.setsDoneToday.lastOrNull()?.repetitions ?: maxReps)
    }

    WeightSelector(
        weightKg = step.plannedWeightKg,
        stepKg = step.exercise.weightStepKg,
        onChange = onWeightChange,
        supportingText = buildString {
            append("Dernière séance : ${formatWeight(step.lastSessionWeightKg)}")
            append("   ·   appui long : demi-pas")
        },
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    RepsSelector(
        selected = reps,
        minReps = minReps,
        maxReps = maxReps,
        onSelect = { reps = it },
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    ValidateButton(text = "VALIDER LA SÉRIE", onClick = { onValidate(reps) })
}

@Composable
private fun RepsOnlyControls(step: CurrentStep, onValidate: (Int) -> Unit) {
    val minReps = step.exercise.targetRepsMin ?: 3
    val maxReps = step.exercise.targetRepsMax ?: 5
    var reps by remember(step.exerciseSessionId, step.setNumber) { mutableIntStateOf(minReps) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        (minReps..maxReps).forEach { value ->
            BigChoice(
                value = value,
                selected = value == reps,
                onClick = { reps = value },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    ValidateButton(text = "VALIDER", onClick = { onValidate(reps) })
}

@Composable
private fun BigChoice(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier.height(84.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$value", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun TimedControls(
    step: CurrentStep,
    onStart: () -> Unit,
    onMarkDone: () -> Unit,
) {
    val seconds = step.exercise.targetDurationSeconds ?: 60

    WorkoutPrimaryButton(
        text = "COMMENCER  ·  ${seconds / 60}:${"%02d".format(seconds % 60)}",
        onClick = onStart,
        icon = Icons.Rounded.PlayArrow,
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.md))

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        WorkoutTextButton(text = "Marquer comme fait", onClick = onMarkDone)
    }
}

/** Le bouton de validation grossit brièvement à l'appui : la série est prise en compte. */
@Composable
private fun ValidateButton(text: String, onClick: () -> Unit) {
    WorkoutPrimaryButton(text = text, onClick = onClick, icon = Icons.Rounded.Check)
}

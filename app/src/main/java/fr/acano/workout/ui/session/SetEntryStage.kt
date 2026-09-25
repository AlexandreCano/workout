package fr.acano.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.targetLabel
import fr.acano.workout.ui.components.ExerciseHeader
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.RepsSelector
import fr.acano.workout.ui.components.WeightSelector
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTextButton
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
    canUndo: Boolean,
    onWeightChange: (Double) -> Unit,
    onValidateReps: (Int) -> Unit,
    onStartEffort: () -> Unit,
    onMarkTimedDone: () -> Unit,
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
            targetLabel = targetLabel(step.targetRepsMin, step.targetRepsMax, step.targetDurationSeconds),
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        // Cadre carré : les animations d'exercice sont quasi toujours carrées, et
        // un cadre 16:10 les faisait flotter entre deux bandes vides. La hauteur
        // est bornée pour garder le bouton de validation au-dessus de la ligne de
        // flottaison. ContentScale.Fit gère proprement une source non carrée.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ExerciseImage(
                exercise = step.exercise,
                modifier = Modifier
                    .heightIn(max = 260.dp)
                    .aspectRatio(1f),
                shape = MaterialTheme.shapes.large,
            )
        }

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
                    text = stringResource(R.string.session_undo_last_set),
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
            text = stringResource(R.string.session_already_done),
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
                    // Même écriture que partout ailleurs : « 12 reps », « 0:45 ».
                    text = set.repetitions?.let { pluralStringResource(R.plurals.session_reps_count, it, it) }
                        ?: set.durationSeconds?.let { "%d:%02d".format(it / 60, it % 60) }
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
    val minReps = step.targetRepsMin ?: 8
    val maxReps = step.targetRepsMax ?: 12
    var reps by remember(step.exerciseSessionId, step.setNumber) {
        mutableIntStateOf(step.setsDoneToday.lastOrNull()?.repetitions ?: maxReps)
    }

    WeightSelector(
        weightKg = step.plannedWeightKg,
        stepKg = step.exercise.weightStepKg,
        onChange = onWeightChange,
        supportingText = stringResource(
            R.string.session_last_session_weight,
            formatWeight(step.lastSessionWeightKg, LocalWeightUnit.current),
        ) + "   ·   " + stringResource(R.string.session_long_press_hint),
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    RepsSelector(
        selected = reps,
        minReps = minReps,
        maxReps = maxReps,
        onSelect = { reps = it },
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    ValidateButton(text = stringResource(R.string.session_validate_set), onClick = { onValidate(reps) })
}

@Composable
private fun RepsOnlyControls(step: CurrentStep, onValidate: (Int) -> Unit) {
    val minReps = step.targetRepsMin ?: 3
    val maxReps = step.targetRepsMax ?: 5
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

    ValidateButton(text = stringResource(R.string.session_validate), onClick = { onValidate(reps) })
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
    val seconds = step.targetDurationSeconds ?: 60

    WorkoutPrimaryButton(
        text = "${stringResource(R.string.session_start)}  ·  ${seconds / 60}:${"%02d".format(seconds % 60)}",
        onClick = onStart,
        icon = Icons.Rounded.PlayArrow,
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.md))

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        WorkoutTextButton(text = stringResource(R.string.session_mark_done), onClick = onMarkDone)
    }
}

/** Le bouton de validation grossit brièvement à l'appui : la série est prise en compte. */
@Composable
private fun ValidateButton(text: String, onClick: () -> Unit) {
    WorkoutPrimaryButton(text = text, onClick = onClick, icon = Icons.Rounded.Check)
}

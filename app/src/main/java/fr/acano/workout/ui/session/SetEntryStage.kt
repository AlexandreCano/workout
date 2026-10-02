package fr.acano.workout.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import fr.acano.workout.ui.theme.WorkoutMotion
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.domain.SetEffort
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatDistance
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.targetLabel
import fr.acano.workout.ui.components.DistanceSelector
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
    onValidateReps: (Int, SetEffort?) -> Unit,
    onValidateDistance: (Double, SetEffort?) -> Unit,
    onStartEffort: () -> Unit,
    onMarkTimedDone: () -> Unit,
    onUndo: () -> Unit,
    onAddSet: () -> Unit,
    onSkipExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Le ressenti est propre à la série en cours : il repart à vide à chaque série.
    var effort by remember(step.exerciseSessionId, step.setNumber) { mutableStateOf<SetEffort?>(null) }
    var confirmSkip by remember(step.exerciseSessionId) { mutableStateOf(false) }

    if (confirmSkip) {
        SkipExerciseDialog(
            exerciseName = step.exercise.name,
            onConfirm = {
                confirmSkip = false
                onSkipExercise()
            },
            onDismiss = { confirmSkip = false },
        )
    }

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
            targetLabel = targetLabel(
                step.targetRepsMin,
                step.targetRepsMax,
                step.targetDurationSeconds,
                step.targetDistanceMeters.takeIf { step.exercise.kind.targetsDistance },
            ),
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

        // La charge d'abord quand l'exercice en a une, puis ce qui se compte.
        val kind = step.exercise.kind
        if (kind.hasWeight) {
            SessionWeightSelector(step, onWeightChange)
            Spacer(Modifier.height(WorkoutTheme.spacing.xl))
        }
        val effortPicker: @Composable () -> Unit = {
            EffortPicker(selected = effort, onSelect = { effort = it })
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
        }
        when {
            kind.hasReps && kind.hasWeight -> WeightedRepsControls(step, effortPicker) { onValidateReps(it, effort) }
            kind.hasReps -> RepsOnlyControls(step, effortPicker) { onValidateReps(it, effort) }
            // Tapis, rameur : le chrono d'abord, la distance est demandée à la fin.
            kind.isTimed -> TimedControls(step, onStartEffort, onMarkTimedDone)
            kind.hasDistance -> DistanceControls(step, effortPicker) { onValidateDistance(it, effort) }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.sm))
        // Les écarts au plan : une série de plus, ou l'exercice passé (machine prise).
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            WorkoutTextButton(
                text = stringResource(R.string.session_add_set),
                onClick = onAddSet,
                icon = Icons.Rounded.Add,
            )
            WorkoutTextButton(
                text = stringResource(R.string.session_skip_exercise),
                onClick = { confirmSkip = true },
                icon = Icons.Rounded.SkipNext,
            )
        }

        if (canUndo) {
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
                    // Même écriture que partout ailleurs : « 12 reps », « 0:45 », « 30 m ».
                    text = set.repetitions?.let { pluralStringResource(R.plurals.session_reps_count, it, it) }
                        ?: set.distanceMeters?.let { formatDistance(it) }
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
private fun SessionWeightSelector(step: CurrentStep, onWeightChange: (Double) -> Unit) {
    WeightSelector(
        weightKg = step.plannedWeightKg,
        stepKg = step.exercise.weightStepKg,
        onChange = onWeightChange,
        supportingText = lastSessionLabel(step) + "   ·   " + stringResource(R.string.session_long_press_hint),
    )
}

/** « Dernière séance : 40 kg », complété du ressenti noté ce jour-là s'il y en a un. */
@Composable
private fun lastSessionLabel(step: CurrentStep): String {
    val weight = formatWeight(step.lastSessionWeightKg, LocalWeightUnit.current)
    val effort = step.lastSessionEffort?.takeIf { step.lastSessionWeightKg != null }
        ?: return stringResource(R.string.session_last_session_weight, weight)
    val hint = stringResource(
        when (effort) {
            SetEffort.EASY -> R.string.session_effort_easy_hint
            SetEffort.OK -> R.string.session_effort_ok_hint
            SetEffort.HARD -> R.string.session_effort_hard_hint
        },
    )
    return stringResource(R.string.session_last_session_weight_effort, weight, hint)
}

/**
 * Le ressenti de la série, facultatif : un sélecteur segmenté pleine largeur,
 * centré comme le reste de l'écran. Un appui choisit, un second enlève le
 * choix ; rien n'est sélectionné par défaut et la validation n'en dépend pas.
 */
@Composable
private fun EffortPicker(selected: SetEffort?, onSelect: (SetEffort?) -> Unit) {
    Text(
        text = stringResource(R.string.session_effort_title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(WorkoutTheme.spacing.sm))
    Row(
        horizontalArrangement = Arrangement.spacedBy(EFFORT_GAP),
        modifier = Modifier
            .fillMaxWidth()
            .height(EFFORT_HEIGHT)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(EFFORT_GAP),
    ) {
        SetEffort.entries.forEach { effort ->
            EffortSegment(
                effort = effort,
                selected = effort == selected,
                onClick = { onSelect(effort.takeUnless { it == selected }) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EffortSegment(effort: SetEffort, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
        animationSpec = WorkoutMotion.effects(),
        label = "effortContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        animationSpec = WorkoutMotion.effects(),
        label = "effortContent",
    )
    val (icon, label) = when (effort) {
        SetEffort.EASY -> Icons.Rounded.SentimentVerySatisfied to R.string.session_effort_easy
        SetEffort.OK -> Icons.Rounded.SentimentNeutral to R.string.session_effort_ok
        SetEffort.HARD -> Icons.Rounded.SentimentVeryDissatisfied to R.string.session_effort_hard
    }
    Surface(
        selected = selected,
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = modifier.fillMaxHeight(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(WorkoutTheme.spacing.sm))
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun SkipExerciseDialog(exerciseName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.session_skip_title, exerciseName)) },
        text = { Text(stringResource(R.string.session_skip_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.session_skip_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.session_skip_cancel)) } },
    )
}

@Composable
private fun WeightedRepsControls(
    step: CurrentStep,
    effortPicker: @Composable () -> Unit,
    onValidate: (Int) -> Unit,
) {
    val minReps = step.targetRepsMin ?: 8
    val maxReps = step.targetRepsMax ?: 12
    var reps by remember(step.exerciseSessionId, step.setNumber) {
        mutableIntStateOf(step.setsDoneToday.lastOrNull()?.repetitions ?: maxReps)
    }

    RepsSelector(
        selected = reps,
        minReps = minReps,
        maxReps = maxReps,
        onSelect = { reps = it },
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    effortPicker()

    ValidateButton(text = stringResource(R.string.session_validate_set), onClick = { onValidate(reps) })
}

/**
 * Répétitions sans charge. Une fourchette courte (3 à 5 vacuums) tient en
 * grosses cases sur une ligne ; au-delà (10 à 15 pompes), on reprend le
 * sélecteur défilant des exercices chargés.
 */
@Composable
private fun RepsOnlyControls(step: CurrentStep, effortPicker: @Composable () -> Unit, onValidate: (Int) -> Unit) {
    val minReps = step.targetRepsMin ?: 3
    val maxReps = step.targetRepsMax ?: 5
    // Une case de plus pour dépasser l'objectif : elle compte dans la largeur.
    val compact = maxReps - minReps + 1 < MAX_BIG_CHOICES
    var reps by remember(step.exerciseSessionId, step.setNumber) {
        mutableIntStateOf(if (compact) minReps else step.setsDoneToday.lastOrNull()?.repetitions ?: maxReps)
    }

    if (compact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            (minReps..maxReps).forEach { value ->
                BigChoice(
                    label = "$value",
                    selected = value == reps,
                    onClick = { reps = value },
                    modifier = Modifier.weight(1f),
                )
            }
            // Au-delà de l'objectif : chaque appui ajoute une répétition.
            BigChoice(
                label = if (reps > maxReps) "$reps" else "+",
                selected = reps > maxReps,
                onClick = { reps = if (reps > maxReps) reps + 1 else maxReps + 1 },
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        RepsSelector(selected = reps, minReps = minReps, maxReps = maxReps, onSelect = { reps = it })
    }

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    effortPicker()

    ValidateButton(text = stringResource(R.string.session_validate), onClick = { onValidate(reps) })
}

@Composable
private fun BigChoice(
    label: String,
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
            Text(label, style = MaterialTheme.typography.headlineMedium)
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

/** Farmer carry, traîneau : la distance parcourue, pré-remplie avec la précédente. */
@Composable
private fun DistanceControls(step: CurrentStep, effortPicker: @Composable () -> Unit, onValidate: (Double) -> Unit) {
    var meters by remember(step.exerciseSessionId, step.setNumber) {
        mutableDoubleStateOf(step.suggestedDistanceMeters ?: DEFAULT_DISTANCE_METERS)
    }

    DistanceSelector(
        meters = meters,
        onChange = { meters = it },
        supportingText = step.lastSessionDistanceMeters?.let {
            stringResource(R.string.session_last_session_weight, formatDistance(it))
        },
    )

    Spacer(Modifier.height(WorkoutTheme.spacing.xl))

    effortPicker()

    ValidateButton(text = stringResource(R.string.session_validate_set), onClick = { onValidate(meters) })
}

/** Le bouton de validation grossit brièvement à l'appui : la série est prise en compte. */
@Composable
private fun ValidateButton(text: String, onClick: () -> Unit) {
    WorkoutPrimaryButton(text = text, onClick = onClick, icon = Icons.Rounded.Check)
}

private val EFFORT_HEIGHT = 56.dp
private val EFFORT_GAP = 4.dp

/** Au-delà de cinq valeurs, les grosses cases deviennent trop étroites pour le pouce. */
private const val MAX_BIG_CHOICES = 5

/** Distance proposée sans cible ni historique : un aller-retour dans la salle. */
internal const val DEFAULT_DISTANCE_METERS = 30.0

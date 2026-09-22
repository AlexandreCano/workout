package fr.acano.workout.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.timer.TimerKind
import fr.acano.workout.ui.common.ExerciseImage
import fr.acano.workout.ui.common.PrimaryActionButton
import fr.acano.workout.ui.common.SecondaryActionButton
import fr.acano.workout.ui.common.WeightStepper
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.targetLabel

@Composable
fun ActiveSessionScreen(
    viewModel: ActiveSessionViewModel,
    onSessionComplete: () -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showQuitDialog by rememberSaveable { mutableStateOf(false) }

    KeepScreenOn()

    // Une série chronométrée se valide toute seule quand le chrono atteint zéro.
    val timer = state.timer
    LaunchedEffect(timer?.isFinished, timer?.kind, state.step?.exerciseSessionId) {
        if (timer != null && timer.isFinished && timer.kind == TimerKind.EFFORT) {
            viewModel.onEffortTimerFinished(context)
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onSessionComplete()
    }

    LaunchedEffect(state.sessionMissing) {
        if (state.sessionMissing) onExit()
    }

    BackHandler { showQuitDialog = true }

    if (showQuitDialog) {
        QuitDialog(
            onKeepGoing = { showQuitDialog = false },
            onPause = {
                showQuitDialog = false
                viewModel.skipTimer(context)
                onExit()
            },
            onAbort = {
                showQuitDialog = false
                viewModel.abortSession(context)
                onExit()
            },
        )
    }

    Scaffold { padding ->
        val step = state.step
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            step == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                SessionHeader(
                    stepIndex = (state.progress?.currentStepIndex ?: 0) + 1,
                    totalSteps = state.progress?.totalSteps ?: 0,
                    onQuit = { showQuitDialog = true },
                )

                Text(
                    text = step.exercise.name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Text(
                    text = if (step.plannedSets > 1) {
                        "Série ${step.setNumber} / ${step.plannedSets}  ·  ${step.exercise.targetLabel()}"
                    } else {
                        step.exercise.targetLabel()
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ExerciseImage(
                    exerciseId = step.exercise.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .aspectRatio(16f / 10f),
                )

                if (step.setsDoneToday.isNotEmpty()) {
                    SetsDoneToday(step)
                }

                Spacer(Modifier.height(20.dp))

                val activeTimer = state.timer
                if (activeTimer != null) {
                    TimerPanel(
                        timer = activeTimer,
                        onPause = { viewModel.pauseTimer(context) },
                        onResume = { viewModel.resumeTimer(context) },
                        onAddThirty = { viewModel.addThirtySeconds(context) },
                        onSkip = { viewModel.skipTimer(context) },
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Pendant la récupération, la saisie de la série suivante attend son tour.
                val restInProgress = activeTimer != null && activeTimer.kind == TimerKind.REST
                if (!restInProgress) {
                    when (step.exercise.kind) {
                        ExerciseKind.WEIGHTED_REPS -> WeightedRepsControls(viewModel, step)
                        ExerciseKind.REPS_ONLY -> RepsOnlyControls(viewModel, step)
                        ExerciseKind.TIMED -> TimedControls(viewModel, step, activeTimer != null)
                    }
                }

                if (step.setsDoneToday.isNotEmpty() || (state.progress?.completedSteps ?: 0) > 0) {
                    UndoButton(onClick = { viewModel.undoLastSet(context) })
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SessionHeader(stepIndex: Int, totalSteps: Int, onQuit: () -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$stepIndex / $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onQuit) {
                Icon(Icons.Rounded.Close, contentDescription = "Quitter la séance")
            }
        }
        LinearProgressIndicator(
            progress = { if (totalSteps == 0) 0f else (stepIndex - 1).toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}

@Composable
private fun SetsDoneToday(step: CurrentStep) {
    val summary = step.setsDoneToday.joinToString("  /  ") { set ->
        set.repetitions?.toString() ?: set.durationSeconds?.let { "${it}s" } ?: "—"
    }
    Text(
        text = "Déjà fait : $summary",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun WeightedRepsControls(viewModel: ActiveSessionViewModel, step: CurrentStep) {
    val context = LocalContext.current
    val minReps = step.exercise.targetRepsMin ?: 8
    val maxReps = step.exercise.targetRepsMax ?: 12
    var reps by remember(step.exerciseSessionId, step.setNumber) {
        mutableStateOf(step.setsDoneToday.lastOrNull()?.repetitions ?: maxReps)
    }

    Text(
        text = "Dernière séance : ${formatWeight(step.lastSessionWeightKg)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    WeightStepper(
        weightKg = step.plannedWeightKg,
        stepKg = step.exercise.weightStepKg,
        onChange = { viewModel.setWeight(context, it) },
        modifier = Modifier.padding(vertical = 16.dp),
    )

    RepsPicker(
        selected = reps,
        minReps = minReps,
        maxReps = maxReps,
        onSelect = { reps = it },
        modifier = Modifier.padding(bottom = 20.dp),
    )

    PrimaryActionButton(
        text = "VALIDER LA SÉRIE",
        onClick = { viewModel.validateRepsSet(context, reps) },
    )
}

@Composable
private fun RepsOnlyControls(viewModel: ActiveSessionViewModel, step: CurrentStep) {
    val context = LocalContext.current
    val minReps = step.exercise.targetRepsMin ?: 3
    val maxReps = step.exercise.targetRepsMax ?: 5
    var reps by remember(step.exerciseSessionId, step.setNumber) { mutableStateOf(minReps) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 20.dp),
    ) {
        (minReps..maxReps).forEach { value ->
            RepsChoiceButton(
                value = value,
                selected = value == reps,
                onClick = { reps = value },
                modifier = Modifier.weight(1f),
            )
        }
    }

    PrimaryActionButton(
        text = "VALIDER",
        onClick = { viewModel.validateRepsSet(context, reps) },
    )
}

@Composable
private fun RepsChoiceButton(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.height(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$value", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun TimedControls(
    viewModel: ActiveSessionViewModel,
    step: CurrentStep,
    timerRunning: Boolean,
) {
    val context = LocalContext.current
    if (timerRunning) return

    val seconds = step.exercise.targetDurationSeconds ?: 60
    Text(
        text = if (step.plannedSets > 1) {
            "Série ${step.setNumber} / ${step.plannedSets}"
        } else {
            "Échauffement"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))

    PrimaryActionButton(
        text = "COMMENCER  ·  ${seconds / 60}:${"%02d".format(seconds % 60)}",
        onClick = { viewModel.startEffortTimer(context) },
    )

    Spacer(Modifier.height(10.dp))

    SecondaryActionButton(
        text = "Marquer comme fait",
        onClick = { viewModel.validateTimedSetManually(context, seconds) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UndoButton(onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
    ) {
        TextButton(onClick = onClick) {
            Icon(Icons.Rounded.Undo, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text("  Annuler la dernière série")
        }
    }
}

@Composable
private fun QuitDialog(
    onKeepGoing: () -> Unit,
    onPause: () -> Unit,
    onAbort: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepGoing,
        title = { Text("Quitter la séance ?") },
        text = {
            Text(
                "La séance est enregistrée au fur et à mesure : tu pourras la reprendre " +
                    "exactement là où tu en es.",
            )
        },
        confirmButton = {
            TextButton(onClick = onPause) { Text("Mettre en pause") }
        },
        dismissButton = {
            TextButton(onClick = onAbort) {
                Text("Abandonner", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

/** L'écran reste allumé pendant la séance : les mains sont sur la machine, pas sur le téléphone. */
@Composable
private fun KeepScreenOn() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

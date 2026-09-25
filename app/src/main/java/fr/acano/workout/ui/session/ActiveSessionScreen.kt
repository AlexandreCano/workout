package fr.acano.workout.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.timer.TimerKind
import fr.acano.workout.ui.components.StepBar
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * L'écran de séance.
 *
 * Il n'a que deux états, et ils s'excluent : soit un chronomètre tourne et il
 * occupe tout l'écran, soit on saisit une série. La bascule entre les deux est
 * un fondu-zoom court — assez pour signaler le changement de contexte, assez
 * bref pour ne jamais faire attendre entre deux séries.
 */
@Composable
fun ActiveSessionScreen(
    viewModel: ActiveSessionViewModel,
    onSessionComplete: () -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showQuitDialog by rememberSaveable { mutableStateOf(false) }
    var reordering by rememberSaveable { mutableStateOf(false) }

    KeepScreenOn()

    val timer = state.timer
    LaunchedEffect(timer?.isFinished, timer?.kind, state.step?.exerciseSessionId) {
        if (timer != null && timer.isFinished && timer.kind == TimerKind.EFFORT) {
            viewModel.onEffortTimerFinished(context)
        }
    }

    LaunchedEffect(state.isFinished) { if (state.isFinished) onSessionComplete() }
    LaunchedEffect(state.sessionMissing) { if (state.sessionMissing) onExit() }

    // L'étape de réorganisation gère elle-même le retour arrière.
    BackHandler(enabled = !reordering) { showQuitDialog = true }

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

    // Pas de Scaffold ici : celui de WorkoutNavHost applique déjà les insets
    // système. Un second Scaffold les appliquerait une deuxième fois et
    // décalerait tout l'écran vers le bas.
    val step = state.step

    if (state.isLoading || step == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            SessionTopBar(
                currentStep = (state.progress?.currentStepIndex ?: 0) + 1,
                totalSteps = state.progress?.totalSteps ?: 0,
                onQuit = { showQuitDialog = true },
                // Rarement utile : une icône discrète plutôt qu'un grand bouton au-dessus de l'exercice.
                onReorder = { reordering = true }.takeIf {
                    !reordering && state.timer == null && state.pendingSteps.size > 1
                },
            )

            AnimatedContent(
                targetState = when {
                    reordering -> Stage.REORDER
                    state.timer != null -> Stage.TIMER
                    else -> Stage.SET_ENTRY
                },
                transitionSpec = {
                    (fadeIn(WorkoutMotion.effects()) + scaleIn(WorkoutMotion.spatial(), initialScale = 0.92f))
                        .togetherWith(
                            fadeOut(WorkoutMotion.fastEffects()) +
                                scaleOut(WorkoutMotion.spatial(), targetScale = 1.04f),
                        )
                },
                label = "sessionStage",
                modifier = Modifier.fillMaxSize(),
            ) { stage ->
                when (stage) {
                    Stage.REORDER -> ReorderStage(
                        steps = state.pendingSteps,
                        onConfirm = { order ->
                            reordering = false
                            viewModel.applyPendingOrder(context, order)
                        },
                        onCancel = { reordering = false },
                    )

                    Stage.TIMER -> state.timer?.let { activeTimer ->
                        TimerStage(
                            timer = activeTimer,
                            caption = state.nextUpCaption(),
                            onPause = { viewModel.pauseTimer(context) },
                            onResume = { viewModel.resumeTimer(context) },
                            onAddThirty = { viewModel.addThirtySeconds(context) },
                            onRemoveThirty = { viewModel.removeThirtySeconds(context) },
                            onSkip = { viewModel.skipTimer(context) },
                        )
                    }

                    Stage.SET_ENTRY -> SetEntryStage(
                        step = step,
                        canUndo = step.setsDoneToday.isNotEmpty(),
                        onWeightChange = { viewModel.setWeight(context, it) },
                        onValidateReps = { viewModel.validateRepsSet(context, it) },
                        onStartEffort = { viewModel.startEffortTimer(context) },
                        onMarkTimedDone = {
                            viewModel.validateTimedSetManually(
                                context,
                                step.targetDurationSeconds ?: 60,
                            )
                        },
                        onUndo = { viewModel.undoLastSet(context) },
                    )
                }
            }
        }
    }
}

/** Les trois états exclusifs de l'écran de séance. */
private enum class Stage { SET_ENTRY, TIMER, REORDER }

/**
 * Légende affichée sous le chronomètre : ce qui vient après.
 * Pendant une récupération, savoir « Chest Press · Série 3/4 » évite de devoir
 * quitter l'écran du chrono pour vérifier.
 */
private fun ActiveSessionUiState.nextUpCaption(): String? {
    val current = step ?: return null
    return when (timer?.kind) {
        TimerKind.REST -> buildString {
            append("Prochaine série\n")
            append(current.exercise.name)
            if (current.plannedSets > 1) {
                append("  ·  Série ${current.setNumber} / ${current.plannedSets}")
            }
        }
        TimerKind.EFFORT -> if (current.plannedSets > 1) {
            "Série ${current.setNumber} / ${current.plannedSets}"
        } else {
            null
        }
        null -> null
    }
}

@Composable
private fun SessionTopBar(currentStep: Int, totalSteps: Int, onQuit: () -> Unit, onReorder: (() -> Unit)?) {
    Column(
        Modifier.padding(
            start = WorkoutTheme.spacing.xl,
            end = WorkoutTheme.spacing.md,
            top = WorkoutTheme.spacing.sm,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$currentStep / $totalSteps",
                style = WorkoutTheme.emphasis.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (onReorder != null) {
                IconButton(onClick = onReorder) {
                    Icon(Icons.Rounded.SwapVert, contentDescription = "Réorganiser la suite")
                }
            }
            IconButton(onClick = onQuit) {
                Icon(Icons.Rounded.Close, contentDescription = "Quitter la séance")
            }
        }
        Spacer(Modifier.height(WorkoutTheme.spacing.xs))
        StepBar(
            currentStep = currentStep,
            totalSteps = totalSteps,
            modifier = Modifier.padding(end = WorkoutTheme.spacing.md),
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.lg))
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
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Quitter la séance ?") },
        text = {
            Text(
                "La séance est enregistrée au fur et à mesure : tu pourras la reprendre " +
                    "exactement là où tu en es.",
            )
        },
        confirmButton = { TextButton(onClick = onPause) { Text("Mettre en pause") } },
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
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

package fr.acano.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.acano.workout.timer.TimerKind
import fr.acano.workout.timer.TimerState
import fr.acano.workout.timer.formatClock
import fr.acano.workout.ui.components.ActionRow
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTimer
import fr.acano.workout.ui.components.WorkoutTonalButton
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * L'écran pendant qu'un chronomètre tourne — récupération comme planche ou vélo.
 *
 * Tout le reste disparaît volontairement : pendant une récupération, la seule
 * décision possible est « j'attends, j'ajuste le temps, ou je repars ». Garder
 * la saisie de la série suivante à l'écran ne ferait qu'inviter à la faute de
 * frappe, la main encore moite.
 */
@Composable
fun TimerStage(
    timer: TimerState,
    caption: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddThirty: () -> Unit,
    onRemoveThirty: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        WorkoutTimer(
            clock = formatClock(timer.remainingMs),
            progress = timer.progress,
            label = timer.stageLabel(),
            finished = timer.isFinished,
            caption = caption,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        if (timer.isFinished) {
            WorkoutPrimaryButton(
                text = if (timer.kind == TimerKind.REST) "SÉRIE SUIVANTE" else "CONTINUER",
                onClick = onSkip,
                icon = Icons.Rounded.PlayArrow,
            )
        } else {
            // Deux rangées : ajuster le temps en haut, décider en bas. Quatre
            // boutons sur une seule ligne tronqueraient « Reprendre ».
            ActionRow {
                WorkoutTonalButton(
                    text = "30 s",
                    onClick = onRemoveThirty,
                    icon = Icons.Rounded.Remove,
                    modifier = Modifier.weight(1f),
                )
                WorkoutTonalButton(
                    text = "30 s",
                    onClick = onAddThirty,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(WorkoutTheme.spacing.sm))
            ActionRow {
                WorkoutTonalButton(
                    text = if (timer.isRunning) "Pause" else "Reprendre",
                    onClick = if (timer.isRunning) onPause else onResume,
                    icon = if (timer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    modifier = Modifier.weight(1f),
                )
                WorkoutTonalButton(
                    text = "Passer",
                    onClick = onSkip,
                    icon = Icons.Rounded.SkipNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.lg))
    }
}

private fun TimerState.stageLabel(): String = when {
    isFinished && kind == TimerKind.REST -> "C'est reparti"
    isFinished -> "Terminé"
    kind == TimerKind.REST -> "Récupération"
    else -> label
}

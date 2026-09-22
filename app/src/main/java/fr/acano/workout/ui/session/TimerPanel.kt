package fr.acano.workout.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.acano.workout.timer.TimerKind
import fr.acano.workout.timer.TimerState
import fr.acano.workout.timer.formatClock
import fr.acano.workout.ui.common.SecondaryActionButton

/**
 * Le chronomètre, très visible : c'est l'information qu'on lit de loin, poignet sur la machine.
 * Le même panneau sert à la récupération et aux exercices chronométrés.
 */
@Composable
fun TimerPanel(
    timer: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddThirty: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent by animateColorAsState(
        targetValue = if (timer.isFinished) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "timerAccent",
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = when {
                    timer.isFinished && timer.kind == TimerKind.REST -> "C'EST REPARTI"
                    timer.isFinished -> "TERMINÉ"
                    timer.kind == TimerKind.REST -> "REPOS"
                    else -> timer.label.uppercase()
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = formatClock(timer.remainingMs),
                style = MaterialTheme.typography.displayLarge,
                color = accent,
            )

            LinearProgressIndicator(
                progress = { timer.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(top = 4.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                if (!timer.isFinished) {
                    SecondaryActionButton(
                        text = if (timer.isRunning) "Pause" else "Reprendre",
                        onClick = if (timer.isRunning) onPause else onResume,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryActionButton(
                        text = "+30 s",
                        onClick = onAddThirty,
                        modifier = Modifier.weight(1f),
                    )
                }
                SecondaryActionButton(
                    text = if (timer.isFinished) "Continuer" else "Passer",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

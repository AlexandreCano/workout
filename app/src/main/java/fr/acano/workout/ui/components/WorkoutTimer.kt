package fr.acano.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Le chronomètre, traité comme un état d'écran et non comme un composant parmi
 * d'autres : pendant une récupération ou une planche, c'est la seule information
 * qui compte.
 *
 * L'anneau se vide dans le sens horaire et double le décompte chiffré — on lit
 * « il reste peu » d'un coup d'œil, sans déchiffrer les secondes. À zéro,
 * l'anneau devient plein, la couleur bascule sur l'accent et l'ensemble pulse
 * doucement : un signal visible même si le téléphone est vibreur coupé.
 */
@Composable
fun WorkoutTimer(
    clock: String,
    progress: Float,
    label: String,
    finished: Boolean,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val accent by animateColorAsState(
        targetValue = if (finished) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = WorkoutMotion.effects(),
        label = "timerAccent",
    )
    val ringColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = WorkoutMotion.effects(),
        label = "timerRing",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val animatedProgress by animateFloatAsState(
        targetValue = if (finished) 1f else progress,
        animationSpec = WorkoutMotion.effects(),
        label = "timerProgress",
    )

    val pulse = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (finished) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timerPulseScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = 340.dp)
                .aspectRatio(1f)
                .scale(pulseScale),
        ) {
            TimerRing(
                progress = animatedProgress,
                ringColor = ringColor,
                trackColor = trackColor,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label.uppercase(),
                    style = WorkoutTheme.emphasis.overline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.sm))
                // « 05:30 » → « 05 minutes 30 secondes » pour TalkBack.
                val spokenClock = clock.split(":").let { parts ->
                    if (parts.size == 2) stringResource(R.string.comp_timer_spoken, parts[0], parts[1]) else clock
                }
                Text(
                    text = clock,
                    style = WorkoutTheme.emphasis.counter,
                    color = accent,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = spokenClock
                    },
                )
            }
        }

        if (caption != null) {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = WorkoutTheme.spacing.xl),
            )
        }
    }
}

@Composable
private fun TimerRing(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.055f
        val inset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        if (progress > 0f) {
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

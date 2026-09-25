package fr.acano.workout.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.history.CaloriesLine
import fr.acano.workout.ui.history.ExerciseSetsBreakdown
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.history.SessionDetailViewModel
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Fin de séance.
 *
 * L'étoile arrive en ressort marqué — c'est le seul moment de l'application où
 * une animation est décorative, et c'est assumé : c'est toute la récompense.
 * Le reste de l'écran est un récapitulatif sobre, lisible sans effort après
 * quarante-cinq minutes d'effort.
 */
@Composable
fun SessionSummaryScreen(
    viewModel: SessionDetailViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.finishIfNeeded() }
    BackHandler { onDone() }

    // Les insets système sont déjà appliqués par le Scaffold de WorkoutNavHost.
    Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        AwardedStar()

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        Text(
            text = "Séance terminée",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.sm))

        Text(
            text = "+1 étoile",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryMetric(
                value = formatDuration(state.durationMillis),
                label = "Durée",
            )
            SummaryMetric(
                value = "${state.lines.size}",
                label = "Exercices",
            )
            SummaryMetric(
                value = "${state.lines.sumOf { it.sets.size }}",
                label = "Séries",
            )
            state.totalKcal?.let {
                SummaryMetric(value = "≈ ${kotlin.math.round(it).toInt()}", label = "kcal")
            }
        }
        if (state.needsProfileForCalories) {
            Spacer(Modifier.height(WorkoutTheme.spacing.md))
            CaloriesLine(totalKcal = null, needsProfile = true)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        SectionHeader(state.title, Modifier.fillMaxWidth())

        state.lines.forEachIndexed { index, line ->
            if (index > 0) RowDivider()
            ExerciseSetsBreakdown(line)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        WorkoutPrimaryButton(text = "TERMINER", onClick = onDone)

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
    }
}

/** L'étoile gagnée : ressort ample et léger pivot, une seule fois, à l'arrivée. */
@Composable
private fun AwardedStar() {
    var revealed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.2f,
        animationSpec = WorkoutMotion.celebratory(),
        label = "awardScale",
    )
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 0f else -35f,
        animationSpec = WorkoutMotion.celebratory(),
        label = "awardRotation",
    )

    LaunchedEffect(Unit) {
        revealed = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = "Étoile obtenue",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .size(128.dp)
                .scale(scale)
                .rotate(rotation),
        )
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


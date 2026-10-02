package fr.acano.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.acano.workout.R
import fr.acano.workout.ui.common.formatDistance
import fr.acano.workout.ui.components.DistanceSelector
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTextButton
import fr.acano.workout.ui.components.durationLabel
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Après le chrono d'un tapis ou d'un rameur : la machine affiche la distance
 * parcourue, il ne reste qu'à la reporter. La série n'est enregistrée qu'ici,
 * avec sa durée et sa distance ; « Passer » l'enregistre sans distance.
 */
@Composable
fun DistanceEntryStage(
    step: CurrentStep,
    pending: PendingDistance,
    onConfirm: (Double) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var meters by remember(pending) {
        mutableDoubleStateOf(step.suggestedDistanceMeters ?: DEFAULT_CARDIO_METERS)
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        Text(
            text = step.exercise.name + "  ·  " + durationLabel(pending.durationSeconds),
            style = WorkoutTheme.emphasis.overline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.sm))
        Text(
            text = stringResource(R.string.session_distance_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.xs))
        Text(
            text = stringResource(R.string.session_distance_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        DistanceSelector(
            meters = meters,
            onChange = { meters = it },
            supportingText = step.lastSessionDistanceMeters?.let {
                stringResource(R.string.session_last_session_weight, formatDistance(it))
            },
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        WorkoutPrimaryButton(
            text = stringResource(R.string.session_validate),
            onClick = { onConfirm(meters) },
            icon = Icons.Rounded.Check,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.sm))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WorkoutTextButton(text = stringResource(R.string.session_skip), onClick = onSkip)
        }
    }
}

/** Sans historique : deux kilomètres, l'ordre de grandeur d'un cardio de dix minutes. */
private const val DEFAULT_CARDIO_METERS = 2000.0

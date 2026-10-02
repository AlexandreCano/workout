package fr.acano.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.domain.stepDistance
import fr.acano.workout.ui.common.formatDistance
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Sélecteur de distance, sur le modèle du sélecteur de charge : deux cibles
 * au pouce, la valeur au centre, un appui long pour le demi-pas. Le pas
 * grandit avec la distance (voir `distanceStep`).
 */
@Composable
fun DistanceSelector(
    meters: Double?,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val haptics = LocalHapticFeedback.current

    fun apply(up: Boolean, fraction: Double = 1.0) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onChange(stepDistance(meters, up, fraction))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StepperTarget(
                icon = Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.comp_distance_decrease),
                onClick = { apply(up = false) },
                onLongClick = { apply(up = false, fraction = 0.5) },
            )
            val text = meters?.let { formatDistance(it) }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = text?.substringBeforeLast(' ') ?: "—",
                    style = WorkoutTheme.emphasis.metric,
                    color = if (meters == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " " + (text?.substringAfterLast(' ') ?: "m"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            StepperTarget(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.comp_distance_increase),
                onClick = { apply(up = true) },
                onLongClick = { apply(up = true, fraction = 0.5) },
            )
        }

        if (supportingText != null) {
            Spacer(Modifier.height(WorkoutTheme.spacing.sm))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

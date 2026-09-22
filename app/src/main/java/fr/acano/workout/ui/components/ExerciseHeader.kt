package fr.acano.workout.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * En-tête de l'exercice en cours : nom, progression des séries, objectif.
 *
 * Le nom glisse verticalement au changement d'exercice — c'est le seul repère
 * qui dit « tu as changé de machine », et il doit être perçu même en périphérie
 * du regard.
 */
@Composable
fun ExerciseHeader(
    name: String,
    completedSets: Int,
    currentSet: Int,
    totalSets: Int,
    targetLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = name,
            transitionSpec = {
                (slideInVertically { it / 3 } + fadeIn(WorkoutMotion.effects()))
                    .togetherWith(slideOutVertically { -it / 3 } + fadeOut(WorkoutMotion.fastEffects()))
                    .using(SizeTransform(clip = false))
            },
            label = "exerciseName",
        ) { currentName ->
            Text(
                text = currentName,
                style = WorkoutTheme.emphasis.exerciseName,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.md))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.lg),
        ) {
            SetProgress(
                completedSets = completedSets,
                currentSet = currentSet,
                totalSets = totalSets,
            )
            if (targetLabel.isNotEmpty()) {
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

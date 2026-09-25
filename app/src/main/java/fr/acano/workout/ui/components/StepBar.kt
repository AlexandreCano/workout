package fr.acano.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.ui.theme.WorkoutMotion

/**
 * Progression dans la séance, un segment par exercice.
 *
 * Préféré à une barre continue : le nombre de segments dit combien d'exercices
 * il reste, information qu'une barre pleine ne donne pas.
 *
 * Le segment courant est **à moitié rempli** plutôt que d'une couleur
 * intermédiaire. Une teinte intermédiaire (`primaryContainer`) n'offrait que
 * 1,56:1 de contraste contre les segments à venir en thème sombre — donc
 * indistinguable, ce qui vidait le composant de son sens. Le demi-remplissage
 * reprend l'accent plein (10:1) et dit littéralement « en cours ».
 */
@Composable
fun StepBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    if (totalSteps <= 0) return

    val doneColor = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.comp_step_progress, currentStep, totalSteps)
    val trackColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = WorkoutMotion.effects(),
        label = "stepTrack",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(HEIGHT)
            .semantics { contentDescription = description },
    ) {
        repeat(totalSteps) { index ->
            val position = index + 1
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(trackColor, CircleShape),
            ) {
                val fill = when {
                    position < currentStep -> 1f
                    position == currentStep -> 0.5f
                    else -> 0f
                }
                if (fill > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(doneColor, CircleShape),
                    )
                }
            }
        }
    }
}

private val HEIGHT = 6.dp

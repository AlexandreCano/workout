package fr.acano.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.acano.workout.ui.theme.WorkoutMotion

/**
 * Progression des séries d'un exercice : ● ● ○ ○.
 *
 * Se lit plus vite qu'un « Série 3 / 4 » parce que la quantité restante est
 * perçue sans être comptée. La pastille courante est plus grande et animée,
 * ce qui suffit à la distinguer sans ajouter de couleur supplémentaire.
 *
 * L'ensemble est annoncé comme une seule information par TalkBack, sinon la
 * lecture égrène une pastille après l'autre.
 */
@Composable
fun SetProgress(
    completedSets: Int,
    currentSet: Int,
    totalSets: Int,
    modifier: Modifier = Modifier,
) {
    if (totalSets <= 1) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics {
            contentDescription = "Série $currentSet sur $totalSets"
        },
    ) {
        repeat(totalSets) { index ->
            val setNumber = index + 1
            SetDot(
                done = setNumber <= completedSets,
                current = setNumber == currentSet,
            )
        }
    }
}

@Composable
private fun SetDot(done: Boolean, current: Boolean) {
    val size by animateDpAsState(
        targetValue = if (current) 16.dp else 10.dp,
        animationSpec = WorkoutMotion.spatial(),
        label = "setDotSize",
    )
    val color by animateColorAsState(
        targetValue = when {
            done -> MaterialTheme.colorScheme.primary
            current -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = WorkoutMotion.effects(),
        label = "setDotColor",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clearAndSetSemantics { }
            .then(
                if (current && !done) {
                    Modifier.border(3.dp, color, CircleShape)
                } else {
                    Modifier.background(color, CircleShape)
                },
            ),
    )
}

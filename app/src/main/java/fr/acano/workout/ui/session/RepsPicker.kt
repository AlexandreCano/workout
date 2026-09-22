package fr.acano.workout.ui.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Choix du nombre de répétitions réellement effectuées.
 *
 * Une rangée de grosses pastilles plutôt qu'un champ de saisie : en pleine série,
 * un appui vaut mieux qu'un clavier. La plage déborde volontairement l'objectif
 * pour couvrir les séries ratées comme les bonnes surprises.
 */
@Composable
fun RepsPicker(
    selected: Int,
    minReps: Int,
    maxReps: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = ((minReps - 3).coerceAtLeast(1))..(maxReps + 3)
    val scrollState = rememberScrollState()

    LaunchedEffect(selected, range.first) {
        val index = (selected - range.first).coerceAtLeast(0)
        scrollState.animateScrollTo((index * 64 - 120).coerceAtLeast(0))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
    ) {
        range.forEach { reps ->
            val isSelected = reps == selected
            val inTarget = reps in minReps..maxReps
            Surface(
                onClick = { onSelect(reps) },
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    inTarget -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(width = 56.dp, height = 56.dp),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text("$reps", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

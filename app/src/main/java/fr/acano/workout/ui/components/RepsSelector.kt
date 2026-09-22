package fr.acano.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Choix du nombre de répétitions réellement effectuées.
 *
 * Des pastilles plutôt qu'un champ de saisie : en fin de série, un appui vaut
 * mieux qu'un clavier qui masque la moitié de l'écran. La plage déborde
 * l'objectif de trois répétitions de chaque côté pour couvrir aussi bien la
 * série ratée que la bonne surprise, et les valeurs hors objectif sont
 * visuellement plus discrètes sans être inaccessibles.
 */
@Composable
fun RepsSelector(
    selected: Int,
    minReps: Int,
    maxReps: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = ((minReps - 3).coerceAtLeast(1))..(maxReps + 3)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var viewportPx by remember { mutableIntStateOf(0) }

    // Recentre la sélection dans la fenêtre visible.
    // `animateScrollTo` travaille en pixels : convertir depuis les dp est
    // indispensable, sinon le défilement est trois à quatre fois trop court
    // sur un écran dense et la valeur choisie sort du cadre.
    LaunchedEffect(selected, range.first, viewportPx) {
        if (viewportPx == 0) return@LaunchedEffect
        val itemPx = with(density) { (ITEM_SIZE + GAP).toPx() }
        val selectedStart = (selected - range.first) * itemPx
        val centered = selectedStart - (viewportPx - with(density) { ITEM_SIZE.toPx() }) / 2f
        scrollState.animateScrollTo(centered.toInt().coerceAtLeast(0))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewportPx = it.width }
            .horizontalScroll(scrollState)
            .padding(horizontal = WorkoutTheme.spacing.xs),
    ) {
        range.forEach { reps ->
            RepsChip(
                reps = reps,
                selected = reps == selected,
                inTarget = reps in minReps..maxReps,
                onSelect = { onSelect(reps) },
            )
        }
    }
}

@Composable
private fun RepsChip(
    reps: Int,
    selected: Boolean,
    inTarget: Boolean,
    onSelect: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            inTarget -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = WorkoutMotion.fastEffects(),
        label = "repsChipContainer",
    )
    val content by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            inTarget -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = WorkoutMotion.fastEffects(),
        label = "repsChipContent",
    )

    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = Modifier
            .size(ITEM_SIZE)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$reps", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private val ITEM_SIZE = 60.dp
private val GAP = 8.dp

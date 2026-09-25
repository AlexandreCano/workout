package fr.acano.workout.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme
import kotlin.math.roundToInt

/** Un élément déplaçable, identifié de façon stable. */
data class ReorderableItem(
    val id: Long,
    val title: String,
    val subtitle: String,
)

/**
 * Liste réordonnable par glisser-déposer.
 *
 * La poignée est la seule zone qui démarre un glissé : sans elle, il faudrait un
 * appui long, et le moindre défilement de la liste déclencherait un déplacement
 * involontaire — précisément ce qu'on ne veut pas les mains moites.
 *
 * Le glissé étant inutilisable avec TalkBack, chaque ligne expose aussi deux
 * actions d'accessibilité « Monter » et « Descendre ».
 *
 * [onItemClick] rend la ligne cliquable (hors poignée) et [trailing] ajoute une
 * action juste avant la poignée ; les deux sont facultatifs.
 */
@Composable
fun ReorderableList(
    items: List<ReorderableItem>,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightFirst: Boolean = true,
    onItemClick: ((ReorderableItem) -> Unit)? = null,
    trailing: (@Composable (ReorderableItem) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val stepPx = with(density) { (ROW_HEIGHT + ROW_GAP).toPx() }

    // On suit l'**index** de la ligne déplacée, pas son identifiant.
    // Le relire dans la liste après chaque permutation ne marche pas : la liste
    // reçue en paramètre n'est rafraîchie qu'à la recomposition suivante, donc
    // l'index y serait périmé et le glissé décrocherait au premier cran.
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Lus au moment du geste, sans servir de clé à pointerInput : keyer sur
    // l'ordre de la liste redémarrerait le détecteur à chaque permutation.
    val itemCount by rememberUpdatedState(items.size)
    val currentOnMove by rememberUpdatedState(onMove)

    Column(
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
        modifier = modifier.fillMaxWidth(),
    ) {
        items.forEachIndexed { index, item ->
            // `key` est indispensable, pas décoratif : sans lui, Compose réutilise
            // les composables par position. Après une permutation, l'emplacement
            // change d'élément, la clé de `pointerInput` change avec lui et le
            // détecteur de glissé redémarre — le geste s'arrête au premier cran.
            key(item.id) {
            val dragging = index == draggedIndex

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .zIndex(if (dragging) 1f else 0f)
                    .offset { IntOffset(0, if (dragging) dragOffset.roundToInt() else 0) }
                    .semantics {
                        customActions = buildList {
                            if (index > 0) {
                                add(
                                    CustomAccessibilityAction("Monter") {
                                        onMove(index, index - 1); true
                                    },
                                )
                            }
                            if (index < items.lastIndex) {
                                add(
                                    CustomAccessibilityAction("Descendre") {
                                        onMove(index, index + 1); true
                                    },
                                )
                            }
                        }
                    },
            ) {
                ReorderRow(
                    item = item,
                    position = index + 1,
                    dragging = dragging,
                    isNext = highlightFirst && index == 0,
                    onClick = onItemClick?.let { click -> { click(item) } },
                    trailing = trailing?.let { slot -> { slot(item) } },
                    handleModifier = Modifier.pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = {
                                draggedIndex = index
                                dragOffset = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { draggedIndex = null; dragOffset = 0f },
                            onDragCancel = { draggedIndex = null; dragOffset = 0f },
                        ) { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            // Boucle et non simple `if` : un évènement de déplacement
                            // peut couvrir plusieurs lignes d'un coup, et un seul
                            // échange par évènement ferait décrocher le doigt.
                            var moving = draggedIndex ?: return@detectDragGestures
                            while (dragOffset > stepPx / 2 && moving < itemCount - 1) {
                                currentOnMove(moving, moving + 1)
                                moving++
                                dragOffset -= stepPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            while (dragOffset < -stepPx / 2 && moving > 0) {
                                currentOnMove(moving, moving - 1)
                                moving--
                                dragOffset += stepPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            draggedIndex = moving
                        }
                    },
                )
            }
            }
        }
    }
}

@Composable
private fun ReorderRow(
    item: ReorderableItem,
    position: Int,
    dragging: Boolean,
    isNext: Boolean,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    handleModifier: Modifier,
) {
    val elevation by animateFloatAsState(
        targetValue = if (dragging) 8f else 0f,
        animationSpec = WorkoutMotion.fastEffects(),
        label = "reorderElevation",
    )

    Surface(
        color = when {
            dragging -> MaterialTheme.colorScheme.surfaceContainerHighest
            isNext -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (isNext && !dragging) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .shadow(elevation.dp, MaterialTheme.shapes.medium, clip = false),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(
                    if (onClick != null) {
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = WorkoutTheme.spacing.lg),
        ) {
            Text(
                text = "$position",
                style = MaterialTheme.typography.titleMedium,
                color = if (isNext && !dragging) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.width(24.dp),
            )

            Spacer(Modifier.width(WorkoutTheme.spacing.md))

            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNext && !dragging) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            trailing?.invoke()

            Box(
                contentAlignment = Alignment.Center,
                modifier = handleModifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "Déplacer ${item.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val ROW_HEIGHT = 76.dp
private val ROW_GAP = 8.dp

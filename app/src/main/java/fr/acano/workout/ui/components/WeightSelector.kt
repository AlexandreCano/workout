package fr.acano.workout.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.acano.workout.domain.WeightUnit
import fr.acano.workout.domain.stepWeight
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatNumber
import fr.acano.workout.ui.common.formatWeightValue
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Sélecteur de charge.
 *
 * Trois partis pris, tous dictés par l'usage une main au milieu d'une série :
 *  - deux cibles circulaires de 72dp aux extrémités, atteignables au pouce ;
 *  - la valeur occupe tout le centre, en chiffres tabulaires pour ne pas
 *    « sauter » latéralement quand elle change de largeur ;
 *  - un appui long applique le demi-pas (1,25 kg sur une machine réglée par
 *    2,5 kg), avec retour haptique pour confirmer sans regarder.
 *
 * La charge reste en kilogrammes ([weightKg], [stepKg]) ; l'affichage et les pas
 * suivent l'unité choisie dans les réglages (voir [WeightUnit.step]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeightSelector(
    weightKg: Double?,
    stepKg: Double,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val unit = LocalWeightUnit.current
    val step = unit.step(stepKg)

    fun apply(delta: Double) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onChange(stepWeight(weightKg, delta, unit))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            StepperTarget(
                icon = Icons.Rounded.Remove,
                contentDescription = "Diminuer la charge de ${formatNumber(step)} ${unit.spokenName}",
                onClick = { apply(-step) },
                onLongClick = { apply(-step / 2) },
            )

            WeightValue(
                weightKg = weightKg,
                unit = unit,
                modifier = Modifier.weight(1f),
            )

            StepperTarget(
                icon = Icons.Rounded.Add,
                contentDescription = "Augmenter la charge de ${formatNumber(step)} ${unit.spokenName}",
                onClick = { apply(step) },
                onLongClick = { apply(step / 2) },
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

@Composable
private fun WeightValue(weightKg: Double?, unit: WeightUnit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = weightKg,
            transitionSpec = {
                val rising = (targetState ?: 0.0) > (initialState ?: 0.0)
                val direction = if (rising) 1 else -1
                (slideInVertically { direction * it / 2 } + fadeIn(WorkoutMotion.fastEffects()))
                    .togetherWith(
                        slideOutVertically { -direction * it / 2 } + fadeOut(WorkoutMotion.fastEffects()),
                    )
            },
            label = "weightValue",
        ) { value ->
            Text(
                text = value?.let { formatWeightValue(it, unit) } ?: "—",
                style = WorkoutTheme.emphasis.metric,
                // Une charge jamais renseignée reste en teinte secondaire :
                // en pleine taille et en pleine couleur, le tiret se lit comme
                // une valeur barrée plutôt que comme une absence.
                color = if (value == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Text(
            text = " ${unit.symbol}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepperTarget(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = WorkoutMotion.fastSpatial(),
        label = "stepperScale",
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            // Le libellé porte sur le nœud lui-même, pas seulement sur l'action :
            // onClickLabel seul laisse TalkBack annoncer « bouton » sans rien d'autre.
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

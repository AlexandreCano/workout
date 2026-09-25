package fr.acano.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.acano.workout.R
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Titre de section. Remplace les titres de cartes : la hiérarchie vient de la
 * typographie et de l'espace, pas d'un conteneur supplémentaire.
 *
 * En casse normale et en gras discret : des capitales espacées sur chaque
 * section finissaient par crier autant que les titres d'écran.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = WorkoutTheme.spacing.md),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * Tuile de statistique : une valeur dominante, une étiquette discrète.
 * La valeur est un chiffre, donc elle porte tout le poids typographique.
 */
@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Surface(
        color = if (emphasized) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WorkoutTheme.spacing.lg),
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(WorkoutTheme.spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Ligne de liste. Les écrans de consultation empilent des lignes séparées par
 * un filet, pas des cartes : une carte par élément fragmente la lecture.
 */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WorkoutTheme.spacing.lg),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(WorkoutTheme.spacing.lg))
        }
        Column(Modifier.weight(1f), content = content)
        if (trailing != null) {
            Spacer(Modifier.width(WorkoutTheme.spacing.lg))
            trailing()
        }
    }
}

@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier,
    )
}

/** Pastille circulaire portant une icône ou un emoji, en tête de ligne. */
@Composable
fun LeadingBadge(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Indicateur de tendance d'une charge. Le signe et la couleur portent
 * l'information ; la description sémantique la rend lisible par TalkBack.
 */
@Composable
fun TrendIndicator(trend: Int, modifier: Modifier = Modifier) {
    if (trend == 0) return
    val rising = trend > 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (rising) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = stringResource(if (rising) R.string.comp_trend_up else R.string.comp_trend_down),
            tint = if (rising) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(16.dp),
        )
    }
}

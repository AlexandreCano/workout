package fr.acano.workout.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.domain.WorkoutType
import fr.acano.workout.ui.common.formatDay
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.TrendIndicator
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme
import kotlinx.coroutines.launch

/**
 * L'accueil.
 *
 * Trois zones, dans l'ordre d'importance : ce que j'ai accompli (l'étoile),
 * ce que je fais maintenant (les deux lancements), ce que ça donne dans le
 * temps (la progression). Aucune carte décorative : la hiérarchie vient de la
 * taille du texte et de l'espace entre les blocs.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSession: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    fun start(type: WorkoutType) {
        scope.launch { onOpenSession(viewModel.startSession(type)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        StarHero(
            stars = state.stars,
            upperCount = state.upperCount,
            lowerCount = state.lowerCount,
        )

        AnimatedVisibility(
            visible = state.resumable != null,
            enter = fadeIn(WorkoutMotion.effects()) + expandVertically(WorkoutMotion.spatial()),
            exit = fadeOut(WorkoutMotion.fastEffects()) + shrinkVertically(WorkoutMotion.spatial()),
        ) {
            state.resumable?.let { resumable ->
                Column {
                    Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
                    ResumeBlock(resumable, onClick = { onOpenSession(resumable.sessionId) })
                }
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        if (state.resumable == null) {
            WorkoutType.entries.forEach { type ->
                SessionLaunchButton(
                    type = type,
                    stepCount = state.stepsPerSession[type] ?: 0,
                    highlighted = type == state.suggestedType,
                    enabled = true,
                    onClick = { start(type) },
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.md))
            }
        }

        if (state.lastSessionAt != null) {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            LastSessionLine(
                type = state.lastSessionType,
                at = state.lastSessionAt!!,
            )
        }

        if (state.progression.isNotEmpty()) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader("Progression récente")
            state.progression.forEachIndexed { index, line ->
                if (index > 0) RowDivider()
                ProgressionRow(line)
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxxl))
    }
}

/**
 * Le compteur d'étoiles, traité comme le titre de l'écran.
 * L'étoile grossit à l'arrivée d'une nouvelle séance : c'est la seule
 * récompense de l'application, elle mérite d'être vue.
 */
@Composable
private fun StarHero(stars: Int, upperCount: Int, lowerCount: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = WorkoutMotion.celebratory(),
        label = "starScale",
    )

    Column {
        Text(
            text = "Workout",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.lg))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics {
                contentDescription = if (stars <= 1) "$stars séance réalisée" else "$stars séances réalisées"
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale)
                    .clearAndSetSemantics { },
            )
            Spacer(Modifier.width(WorkoutTheme.spacing.md))
            Text(
                text = "$stars",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }

        Text(
            text = when (stars) {
                0 -> "Aucune séance pour l'instant"
                1 -> "séance réalisée"
                else -> "séances réalisées"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (stars > 0) {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.lg),
                modifier = Modifier.height(32.dp),
            ) {
                SplitCount(upperCount, WorkoutType.UPPER_BODY.label)
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SplitCount(lowerCount, WorkoutType.LOWER_BODY.label)
            }
        }
    }
}

@Composable
private fun SplitCount(count: Int, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(WorkoutTheme.spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

/**
 * Bouton de lancement d'une séance : l'élément le plus large de l'écran.
 * Celui qui est suggéré prend le conteneur accentué, l'autre reste neutre —
 * on distingue les deux sans les mettre en compétition.
 */
@Composable
private fun SessionLaunchButton(
    type: WorkoutType,
    stepCount: Int,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = WorkoutTheme.spacing.xl),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.xs))
                Text(
                    text = "$stepCount étapes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (highlighted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Surface(
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** Reprise d'une séance interrompue : prend la place des boutons de lancement. */
@Composable
private fun ResumeBlock(resumable: ResumableSession, onClick: () -> Unit) {
    Column {
        SectionHeader("Séance en cours")

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(WorkoutTheme.spacing.xl)) {
                Text(resumable.type.label, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(WorkoutTheme.spacing.xs))
                Text(
                    text = buildString {
                        append(resumable.exerciseName)
                        if (resumable.plannedSets > 1) {
                            append("  ·  Série ${resumable.setNumber} / ${resumable.plannedSets}")
                        }
                        append("  ·  ${resumable.stepIndex} / ${resumable.totalSteps}")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.xl))
                WorkoutPrimaryButton(
                    text = "REPRENDRE",
                    onClick = onClick,
                    icon = Icons.Rounded.PlayArrow,
                )
            }
        }
    }
}

@Composable
private fun LastSessionLine(type: WorkoutType?, at: Long) {
    Text(
        text = "Dernière séance  ·  ${type?.label.orEmpty()}  ·  ${formatDay(at)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProgressionRow(line: ProgressionLine) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WorkoutTheme.spacing.lg),
    ) {
        Text(
            text = line.exerciseName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatWeight(line.weightKg),
            style = WorkoutTheme.emphasis.metricSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(WorkoutTheme.spacing.sm))
        TrendIndicator(line.trend)
    }
}

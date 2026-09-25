package fr.acano.workout.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatDate
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.components.ListRow
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.TrendIndicator
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTonalButton
import fr.acano.workout.ui.theme.WorkoutMotion
import fr.acano.workout.ui.theme.WorkoutTheme
import kotlinx.coroutines.launch

/**
 * L'accueil.
 *
 * Trois zones, dans l'ordre d'importance : ce que j'ai accompli (l'étoile),
 * ce que je fais maintenant (les lancements), ce que ça donne dans le
 * temps (la progression). Aucune carte décorative : la hiérarchie vient de la
 * taille du texte et de l'espace entre les blocs.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSession: (Long) -> Unit,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Pendant une séance, la liste des entraînements est repliée : on ne peut rien en lancer.
    var workoutsExpanded by rememberSaveable { mutableStateOf(false) }

    fun start(workoutId: Long) {
        scope.launch { onOpenSession(viewModel.startSession(workoutId)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        StarHero(state.stars, onOpenSettings)

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

        val inSession = state.resumable != null
        if (inSession) {
            // Replié : une seule ligne, qu'on déplie pour préparer ou modifier un entraînement.
            ListRow(
                modifier = Modifier.clickable { workoutsExpanded = !workoutsExpanded },
                trailing = {
                    Icon(
                        if (workoutsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (workoutsExpanded) "Replier" else "Déplier",
                    )
                },
            ) {
                Text(
                    text = if (state.workouts.size > 1) "${state.workouts.size} entraînements" else "Entraînements",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Termine la séance en cours pour en lancer un autre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SectionHeader("Entraînements")
        }

        AnimatedVisibility(
            visible = !inSession || workoutsExpanded,
            enter = fadeIn(WorkoutMotion.effects()) + expandVertically(WorkoutMotion.spatial()),
            exit = fadeOut(WorkoutMotion.fastEffects()) + shrinkVertically(WorkoutMotion.spatial()),
        ) {
            Column {
                // L'entraînement suggéré en grand ; les autres en lignes compactes,
                // pour que la liste reste courte même avec beaucoup d'entraînements.
                val suggested = state.workouts.firstOrNull { !inSession && it.id == state.suggestedWorkoutId }
                if (suggested != null) {
                    SessionLaunchButton(
                        workout = suggested,
                        enabled = suggested.stepCount > 0,
                        onClick = { start(suggested.id) },
                        onEdit = { onEditWorkout(suggested.id) },
                    )
                    Spacer(Modifier.height(WorkoutTheme.spacing.sm))
                }
                state.workouts.filter { it != suggested }.forEachIndexed { index, workout ->
                    if (index > 0) RowDivider()
                    WorkoutRow(
                        workout = workout,
                        // Un entraînement vidé (son seul exercice a été supprimé) ne se lance pas.
                        canStart = !inSession && workout.stepCount > 0,
                        onStart = { start(workout.id) },
                        onEdit = { onEditWorkout(workout.id) },
                    )
                }
                Spacer(Modifier.height(WorkoutTheme.spacing.md))
                WorkoutTonalButton(
                    text = "Créer un entraînement",
                    onClick = onCreateWorkout,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.progression.isNotEmpty()) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader("Progression récente")
            state.progression.forEachIndexed { index, line ->
                if (index > 0) RowDivider()
                ProgressionRow(line, onClick = { onOpenExercise(line.exerciseId) })
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
private fun StarHero(stars: Int, onOpenSettings: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = WorkoutMotion.celebratory(),
        label = "starScale",
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Workout",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Réglages",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
    }
}

/**
 * L'entraînement suggéré : l'élément le plus large de l'écran, en conteneur
 * accentué. C'est le bouton qu'on vise en arrivant à la salle.
 */
@Composable
private fun SessionLaunchButton(
    workout: WorkoutLaunch,
    enabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val title = workout.name
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    text = "Suggéré",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = workout.subtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Modifier $title")
            }
            Spacer(Modifier.width(WorkoutTheme.spacing.sm))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

/** Un autre entraînement : une ligne, qu'on lance d'un tap ou qu'on modifie au crayon. */
@Composable
private fun WorkoutRow(
    workout: WorkoutLaunch,
    canStart: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
) {
    ListRow(
        modifier = Modifier.clickable(enabled = canStart, onClickLabel = "Lancer ${workout.name}", onClick = onStart),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Modifier ${workout.name}")
                }
                if (canStart) {
                    FilledTonalIconButton(onClick = onStart) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Lancer ${workout.name}")
                    }
                }
            }
        },
    ) {
        Text(workout.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = workout.subtitle(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** « 7 étapes · il y a 3 jours », ou « 7 étapes · jamais fait ». */
private fun WorkoutLaunch.subtitle(): String = buildString {
    append(if (stepCount > 1) "$stepCount étapes" else "$stepCount étape")
    append("  ·  ")
    append(lastDoneAt?.let { formatDate(it) } ?: "jamais fait")
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
                Text(resumable.title, style = MaterialTheme.typography.headlineSmall)
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
private fun ProgressionRow(line: ProgressionLine, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Voir ${line.exerciseName}", onClick = onClick)
            .padding(vertical = WorkoutTheme.spacing.lg),
    ) {
        Text(
            text = line.exerciseName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatWeight(line.weightKg, LocalWeightUnit.current),
            style = WorkoutTheme.emphasis.metricSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(WorkoutTheme.spacing.sm))
        TrendIndicator(line.trend)
    }
}

private const val DISABLED_ALPHA = 0.38f

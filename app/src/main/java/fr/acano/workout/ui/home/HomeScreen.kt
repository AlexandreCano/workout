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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.R
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
                        contentDescription = stringResource(if (workoutsExpanded) R.string.home_collapse else R.string.home_expand),
                    )
                },
            ) {
                Text(
                    text = if (state.workouts.size > 1) {
                        pluralStringResource(R.plurals.home_workouts_count, state.workouts.size, state.workouts.size)
                    } else {
                        stringResource(R.string.home_workouts)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.home_finish_session_first),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SectionHeader(stringResource(R.string.home_workouts))
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
                    text = stringResource(R.string.home_create_workout),
                    onClick = onCreateWorkout,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.progression.isNotEmpty()) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader(stringResource(R.string.home_recent_progress))
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

    val starsDescription = pluralStringResource(R.plurals.home_stars_description, stars, stars)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.lg))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics {
                contentDescription = starsDescription
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
                0 -> stringResource(R.string.home_no_session_yet)
                else -> pluralStringResource(R.plurals.home_stars_caption, stars)
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
                    text = stringResource(R.string.home_suggested),
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
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.home_edit_workout, title))
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
    val startLabel = stringResource(R.string.home_start_workout, workout.name)
    ListRow(
        modifier = Modifier.clickable(enabled = canStart, onClickLabel = startLabel, onClick = onStart),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.home_edit_workout, workout.name))
                }
                if (canStart) {
                    FilledTonalIconButton(onClick = onStart) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = startLabel)
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
@Composable
private fun WorkoutLaunch.subtitle(): String {
    val steps = pluralStringResource(R.plurals.home_step_count, stepCount, stepCount)
    val lastDone = lastDoneAt?.let { formatDate(it) } ?: stringResource(R.string.home_never_done)
    return "$steps  ·  $lastDone"
}

/** Reprise d'une séance interrompue : prend la place des boutons de lancement. */
@Composable
private fun ResumeBlock(resumable: ResumableSession, onClick: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.home_session_in_progress))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(WorkoutTheme.spacing.xl)) {
                Text(resumable.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(WorkoutTheme.spacing.xs))
                val setLabel = stringResource(R.string.home_resume_set, resumable.setNumber, resumable.plannedSets)
                Text(
                    text = buildString {
                        append(resumable.exerciseName)
                        if (resumable.plannedSets > 1) {
                            append("  ·  $setLabel")
                        }
                        append("  ·  ${resumable.stepIndex} / ${resumable.totalSteps}")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.xl))
                WorkoutPrimaryButton(
                    text = stringResource(R.string.home_resume),
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
            .clickable(onClickLabel = stringResource(R.string.home_view_exercise, line.exerciseName), onClick = onClick)
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

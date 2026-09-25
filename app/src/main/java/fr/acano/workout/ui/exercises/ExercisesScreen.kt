package fr.acano.workout.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.R
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatDate
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.label
import fr.acano.workout.ui.common.locale
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.WorkoutTonalButton
import fr.acano.workout.ui.history.BackRow
import fr.acano.workout.ui.history.ScreenTitle
import fr.acano.workout.ui.history.SetTiles
import fr.acano.workout.ui.theme.WorkoutTheme

/**
 * Catalogue des exercices, en deux parties : ceux que l'utilisateur a ajoutés,
 * qu'il peut modifier ou supprimer, puis ceux fournis avec l'application. Une
 * recherche en tête, pour quand la liste s'allonge.
 *
 * Chaque ligne répond aux deux questions qu'on se pose devant une machine :
 * « j'avais mis combien la dernière fois ? » — la charge, en gros — et « je
 * l'ai fait quand ? ».
 */
@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel,
    onOpenExercise: (String) -> Unit,
    onCreateExercise: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searching = state.query.isNotBlank()

    LazyColumn(
        contentPadding = PaddingValues(
            start = WorkoutTheme.spacing.xl,
            end = WorkoutTheme.spacing.xl,
            top = WorkoutTheme.spacing.xl,
            bottom = WorkoutTheme.spacing.xxxl,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenTitle(stringResource(R.string.tab_exercises))
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.exercises_search_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.exercises_clear_search))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(WorkoutTheme.spacing.xl))
        }

        if (searching && state.custom.isEmpty() && state.builtIn.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.exercises_no_match, state.query.trim()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@LazyColumn
        }

        if (!searching || state.custom.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.exercises_mine))
                if (!state.hasCustom) {
                    Text(
                        text = stringResource(R.string.exercises_mine_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = WorkoutTheme.spacing.md),
                    )
                }
            }
            items(state.custom, key = { it.exercise.id }) { row ->
                ExerciseRow(row, onClick = { onOpenExercise(row.exercise.id) })
                RowDivider()
            }
        }
        if (!searching) {
            item {
                Spacer(Modifier.height(WorkoutTheme.spacing.md))
                WorkoutTonalButton(
                    text = stringResource(R.string.exercises_create),
                    onClick = onCreateExercise,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            }
        }
        if (state.builtIn.isNotEmpty()) {
            item {
                if (searching && state.custom.isNotEmpty()) Spacer(Modifier.height(WorkoutTheme.spacing.xl))
                SectionHeader(stringResource(R.string.exercises_built_in))
            }
            items(state.builtIn, key = { it.exercise.id }) { row ->
                ExerciseRow(row, onClick = { onOpenExercise(row.exercise.id) })
                RowDivider()
            }
        }
    }
}

@Composable
private fun ExerciseRow(row: ExerciseRow, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = WorkoutTheme.spacing.md),
    ) {
        ExerciseImage(
            exercise = row.exercise,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.width(WorkoutTheme.spacing.lg))

        Column(Modifier.weight(1f)) {
            Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = usageLabel(row.sessionCount, row.lastDoneAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Une charge n'a de sens que pour un exercice chargé.
        if (row.exercise.kind == ExerciseKind.WEIGHTED_REPS) {
            Text(
                text = formatWeight(row.lastWeightKg, LocalWeightUnit.current),
                style = WorkoutTheme.emphasis.metricSmall,
                color = if (row.lastWeightKg != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** « 3 séances · il y a 3 jours », ou « Jamais fait ». */
@Composable
private fun usageLabel(sessionCount: Int, lastDoneAt: Long?): String = when {
    sessionCount == 0 || lastDoneAt == null -> stringResource(R.string.exercises_never_done)
    else -> pluralStringResource(R.plurals.exercises_session_count, sessionCount, sessionCount) +
        "  ·  ${formatDate(lastDoneAt)}"
}

/**
 * Détail d'un exercice : l'essentiel en chiffres d'abord — la dernière charge,
 * la courbe —, puis l'historique séance par séance en tuiles, comme dans le
 * détail d'une séance. L'image est une vignette : elle identifie la machine,
 * elle n'a pas à repousser les chiffres sous le pli.
 */
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercise
    val unit = LocalWeightUnit.current
    val locale = LocalContext.current.locale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        BackRow(onBack)

        if (exercise != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseImage(
                    exercise = exercise,
                    modifier = Modifier.size(112.dp),
                    shape = MaterialTheme.shapes.large,
                )
                Spacer(Modifier.width(WorkoutTheme.spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(WorkoutTheme.spacing.xs))
                    Text(
                        text = exercise.kind.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            if (exercise.isCustom) R.string.exercises_added_by_you else R.string.exercises_built_in_single,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Les exercices de l'application sont décrits par le code : seuls les siens se modifient.
            if (exercise.isCustom) {
                Spacer(Modifier.height(WorkoutTheme.spacing.md))
                WorkoutTonalButton(text = stringResource(R.string.exercises_edit), onClick = onEdit, icon = Icons.Rounded.Edit)
            }
        }

        val lastWeight = state.lastWeightKg
        val lastDate = state.history.firstOrNull()?.date
        if (lastWeight != null && lastDate != null) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xl))
            Text(
                text = formatWeight(lastWeight, unit),
                style = WorkoutTheme.emphasis.metric,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.exercises_last_weight, formatDate(lastDate)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.chartPoints.size >= 2) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader(stringResource(R.string.exercises_weight_progress))
            WeightChart(
                points = state.chartPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        SectionHeader(stringResource(R.string.exercises_history))

        if (state.history.isEmpty()) {
            Text(
                text = stringResource(R.string.exercises_not_done_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.history.forEachIndexed { index, entry ->
            if (index > 0) RowDivider()
            Column(Modifier.padding(vertical = WorkoutTheme.spacing.lg)) {
                Text(
                    text = formatDate(entry.date).replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(WorkoutTheme.spacing.sm))
                SetTiles(entry.sets, entry.bestSetIndex)
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxxl))
    }
}

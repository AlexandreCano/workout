package fr.acano.workout.ui.workouts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.R
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.stepDistance
import fr.acano.workout.ui.common.CatalogFilter
import fr.acano.workout.ui.common.formatDistance
import fr.acano.workout.ui.common.summary
import fr.acano.workout.ui.common.targetLabel
import fr.acano.workout.ui.components.CatalogSearchField
import fr.acano.workout.ui.components.CategoryFilterRow
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.ReorderableItem
import fr.acano.workout.ui.components.ReorderableList
import fr.acano.workout.ui.components.RowDivider
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.StepperRow
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTextButton
import fr.acano.workout.ui.components.WorkoutTonalButton
import fr.acano.workout.ui.components.durationLabel
import fr.acano.workout.ui.components.nextDuration
import fr.acano.workout.ui.components.previousDuration
import fr.acano.workout.ui.history.BackRow
import fr.acano.workout.ui.theme.WorkoutTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Création ou modification d'un entraînement personnalisé : un nom, puis les
 * exercices choisis dans le catalogue, dans l'ordre où on veut les faire.
 *
 * Toucher une ligne règle ses séries et ses reps (ou sa durée) ; la croix la retire ; la
 * poignée la déplace. Le choix des exercices se fait dans une feuille à part
 * pour que la liste principale reste courte et lisible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    viewModel: WorkoutEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var editingKey by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    // Textes du snackbar résolus pendant la composition : il s'affiche depuis une coroutine.
    val context = LocalResources.current
    val undoLabel = stringResource(R.string.workouts_undo)

    // Quitter avec des modifications non enregistrées demande confirmation,
    // qu'on passe par la flèche ou par le geste retour du système.
    fun leave() {
        if (state.hasUnsavedChanges) confirmDiscard = true else onDone()
    }
    BackHandler(enabled = state.hasUnsavedChanges) { confirmDiscard = true }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WorkoutTheme.spacing.xl),
        ) {
            BackRow(::leave)

            // Le formulaire attend la fin du chargement : sinon, ce qu'on saisit dans
            // l'intervalle serait écrasé par les valeurs lues en base.
            if (state.isLoading) return@Column

            Text(
                text = stringResource(if (state.isExisting) R.string.workouts_title_edit else R.string.workouts_title_new),
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(Modifier.height(WorkoutTheme.spacing.xl))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.workouts_name_label)) },
                placeholder = { Text(stringResource(R.string.workouts_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader(stringResource(R.string.workouts_exercises_header))

            if (state.steps.isEmpty()) {
                Text(
                    text = stringResource(R.string.workouts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ReorderableList(
                    items = state.steps.map {
                        ReorderableItem(
                            id = it.key,
                            title = it.exercise.name,
                            subtitle = "${setsLabel(it.step.plannedSets)}  ·  ${it.step.targetLabel()}" +
                                restLabel(it.step.restSeconds),
                        )
                    },
                    onMove = viewModel::move,
                    highlightFirst = false,
                    onItemClick = { editingKey = it.id },
                    trailing = { item ->
                        IconButton(
                            onClick = {
                                val name = viewModel.remove(item.id) ?: return@IconButton
                                // Une croix se touche par erreur : on laisse quelques secondes pour revenir en arrière.
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    val result = snackbar.showSnackbar(context.getString(R.string.workouts_removed, name), actionLabel = undoLabel, duration = SnackbarDuration.Short)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.workouts_remove_description, item.title))
                        }
                    },
                )
            }

            Spacer(Modifier.height(WorkoutTheme.spacing.lg))

            WorkoutTonalButton(
                text = stringResource(R.string.workouts_add_exercises),
                onClick = { showPicker = true },
                icon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

            WorkoutPrimaryButton(
                text = stringResource(R.string.workouts_save),
                onClick = { scope.launch { if (viewModel.save()) onDone() } },
                enabled = state.canSave,
                icon = Icons.Rounded.Check,
            )

            if (state.isExisting) {
                Spacer(Modifier.height(WorkoutTheme.spacing.sm))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WorkoutTextButton(
                        text = stringResource(R.string.workouts_delete_workout),
                        onClick = { confirmDelete = true },
                        icon = Icons.Rounded.Delete,
                    )
                }
            }

            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(WorkoutTheme.spacing.lg))
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.workouts_discard_title)) },
            text = { Text(stringResource(R.string.workouts_discard_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDone() }) { Text(stringResource(R.string.workouts_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.workouts_discard_dismiss)) }
            },
        )
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ExercisePicker(
                catalogue = state.visibleCatalogue,
                categories = state.categories,
                filter = state.filter,
                selectedIds = state.selectedIds,
                onQueryChange = viewModel::setQuery,
                onCategoryChange = viewModel::setCategory,
                onToggle = viewModel::toggleExercise,
                onDone = { showPicker = false },
            )
        }
    }

    state.steps.firstOrNull { it.key == editingKey }?.let { step ->
        StepSettingsDialog(
            step = step,
            onSetsChange = { viewModel.setPlannedSets(step.key, it) },
            onRepsMinChange = { viewModel.setRepsMin(step.key, it) },
            onRepsMaxChange = { viewModel.setRepsMax(step.key, it) },
            onDurationChange = { viewModel.setDuration(step.key, it) },
            onDistanceChange = { viewModel.setDistance(step.key, it) },
            onRestChange = { viewModel.setRest(step.key, it) },
            onDismiss = { editingKey = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.workouts_delete_title, state.name.trim())) },
            text = { Text(stringResource(R.string.workouts_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            viewModel.delete()
                            onDone()
                        }
                    },
                ) { Text(stringResource(R.string.workouts_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.workouts_cancel)) }
            },
        )
    }
}

/**
 * Le catalogue en liste à cocher : cocher ajoute en fin d'entraînement, décocher
 * retire. Une recherche et un filtre par famille, pour s'y retrouver parmi
 * deux cents exercices.
 */
@Composable
private fun ExercisePicker(
    catalogue: List<ExerciseEntity>,
    categories: List<ExerciseCategory>,
    filter: CatalogFilter,
    selectedIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (ExerciseCategory?) -> Unit,
    onToggle: (ExerciseEntity) -> Unit,
    onDone: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WorkoutTheme.spacing.xl),
        ) {
            Text(
                text = stringResource(R.string.workouts_catalog),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDone) { Text(stringResource(R.string.workouts_done)) }
        }

        CatalogSearchField(
            query = filter.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = WorkoutTheme.spacing.xl),
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.md))
        CategoryFilterRow(
            categories = categories,
            selected = filter.category,
            onSelect = onCategoryChange,
            contentPadding = PaddingValues(horizontal = WorkoutTheme.spacing.xl),
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.sm))

        LazyColumn(
            contentPadding = PaddingValues(
                start = WorkoutTheme.spacing.xl,
                end = WorkoutTheme.spacing.xl,
                bottom = WorkoutTheme.spacing.xxl,
            ),
        ) {
            if (catalogue.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.exercises_no_filter_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = WorkoutTheme.spacing.lg),
                    )
                }
            }
            items(catalogue, key = { it.id }) { exercise ->
                val selected = exercise.id in selectedIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(exercise) }
                        .padding(vertical = WorkoutTheme.spacing.sm),
                ) {
                    ExerciseImage(
                        exercise = exercise,
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.small,
                    )
                    Spacer(Modifier.width(WorkoutTheme.spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = exercise.summary(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // La ligne entière est la cible : la case ne fait que refléter l'état.
                    Checkbox(checked = selected, onCheckedChange = null)
                }
                RowDivider()
            }
        }
    }
}

/**
 * Réglages d'un exercice dans cet entraînement : le nombre de séries, la cible
 * propre au type d'exercice — une fourchette de reps, ou une durée — et le repos.
 */
@Composable
private fun StepSettingsDialog(
    step: EditorStep,
    onSetsChange: (Int) -> Unit,
    onRepsMinChange: (Int) -> Unit,
    onRepsMaxChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDistanceChange: (Int) -> Unit,
    onRestChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val vm = WorkoutEditorViewModel
    val planned = step.step
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(step.exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm)) {
                StepperRow(
                    label = stringResource(R.string.workouts_sets),
                    value = "${planned.plannedSets}",
                    onDecrement = { onSetsChange(planned.plannedSets - 1) },
                    onIncrement = { onSetsChange(planned.plannedSets + 1) },
                    canDecrement = planned.plannedSets > vm.MIN_SETS,
                    canIncrement = planned.plannedSets < vm.MAX_SETS,
                )
                val duration = planned.targetDurationSeconds
                if (step.isTimed && duration != null) {
                    StepperRow(
                        label = stringResource(R.string.workouts_duration),
                        value = durationLabel(duration),
                        onDecrement = { onDurationChange(previousDuration(duration)) },
                        onIncrement = { onDurationChange(nextDuration(duration)) },
                        canDecrement = duration > vm.MIN_DURATION_SECONDS,
                        canIncrement = duration < vm.MAX_DURATION_SECONDS,
                    )
                }
                val distance = planned.targetDistanceMeters
                if (step.targetsDistance && distance != null) {
                    StepperRow(
                        label = stringResource(R.string.workouts_distance),
                        value = formatDistance(distance.toDouble()),
                        onDecrement = { onDistanceChange(stepDistance(distance.toDouble(), up = false).roundToInt()) },
                        onIncrement = { onDistanceChange(stepDistance(distance.toDouble(), up = true).roundToInt()) },
                        canDecrement = distance > vm.MIN_DISTANCE_METERS,
                        canIncrement = distance < vm.MAX_DISTANCE_METERS,
                    )
                }
                val min = planned.targetRepsMin
                val max = planned.targetRepsMax
                if (!step.isTimed && !step.targetsDistance && min != null && max != null) {
                    StepperRow(
                        label = stringResource(R.string.workouts_reps_min),
                        value = "$min",
                        onDecrement = { onRepsMinChange(min - 1) },
                        onIncrement = { onRepsMinChange(min + 1) },
                        canDecrement = min > vm.MIN_REPS,
                        canIncrement = min < vm.MAX_REPS,
                    )
                    StepperRow(
                        label = stringResource(R.string.workouts_reps_max),
                        value = "$max",
                        onDecrement = { onRepsMaxChange(max - 1) },
                        onIncrement = { onRepsMaxChange(max + 1) },
                        canDecrement = max > vm.MIN_REPS,
                        canIncrement = max < vm.MAX_REPS,
                    )
                }
                StepperRow(
                    label = stringResource(R.string.workouts_rest),
                    value = if (planned.restSeconds == 0) stringResource(R.string.workouts_rest_none) else durationLabel(planned.restSeconds),
                    onDecrement = { onRestChange(planned.restSeconds - REST_STEP_SECONDS) },
                    onIncrement = { onRestChange(planned.restSeconds + REST_STEP_SECONDS) },
                    canDecrement = planned.restSeconds > 0,
                    canIncrement = planned.restSeconds < vm.MAX_REST_SECONDS,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.workouts_ok)) } },
    )
}

@Composable
private fun setsLabel(sets: Int): String = pluralStringResource(R.plurals.workouts_sets_count, sets, sets)

/** « · repos 1 min », ou rien quand l'étape n'a pas de repos. */
@Composable
private fun restLabel(seconds: Int): String =
    if (seconds > 0) "  ·  " + stringResource(R.string.workouts_rest_suffix, durationLabel(seconds)) else ""

private const val REST_STEP_SECONDS = 15

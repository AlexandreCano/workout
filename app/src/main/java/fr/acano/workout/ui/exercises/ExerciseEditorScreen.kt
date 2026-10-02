package fr.acano.workout.ui.exercises

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.R
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatNumber
import fr.acano.workout.ui.common.label
import fr.acano.workout.ui.components.ExerciseImage
import fr.acano.workout.ui.components.SectionHeader
import fr.acano.workout.ui.components.StepperRow
import fr.acano.workout.ui.components.WorkoutPrimaryButton
import fr.acano.workout.ui.components.WorkoutTextButton
import fr.acano.workout.ui.components.WorkoutTonalButton
import fr.acano.workout.ui.history.BackRow
import fr.acano.workout.ui.theme.WorkoutTheme
import kotlinx.coroutines.launch

/**
 * Création ou modification d'un exercice.
 *
 * L'ordre suit les questions qu'on se pose devant une nouvelle machine : à quoi
 * elle ressemble, comment on l'appelle, comment on la travaille. Les séries,
 * répétitions et repos appartiennent aux entraînements, pas à l'exercice.
 */
@Composable
fun ExerciseEditorScreen(
    viewModel: ExerciseEditorViewModel,
    onDone: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Non nul pendant la confirmation : les entraînements dont l'exercice sera retiré.
    var usedBy by remember { mutableStateOf<List<String>?>(null) }
    val vm = ExerciseEditorViewModel

    // Sélecteur de photos du système : aucune permission de stockage n'est nécessaire.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::pickImage)
    }
    fun launchPicker() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WorkoutTheme.spacing.xl),
    ) {
        BackRow(onDone)

        // Le formulaire attend la fin du chargement : sinon, ce qu'on saisit dans
        // l'intervalle serait écrasé par les valeurs lues en base.
        if (state.isLoading) return@Column

        Text(
            text = stringResource(if (state.isExisting) R.string.exercises_editor_title_edit else R.string.exercises_editor_title_new),
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        // --- Image ---
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ExerciseImage(
                model = state.previewModel,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .size(200.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClickLabel = stringResource(R.string.exercises_choose_image), onClick = ::launchPicker),
            )
        }
        Spacer(Modifier.height(WorkoutTheme.spacing.md))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            WorkoutTonalButton(
                text = stringResource(
                    if (state.previewModel == null) R.string.exercises_choose_image else R.string.exercises_change_image,
                ),
                onClick = ::launchPicker,
                icon = Icons.Rounded.AddPhotoAlternate,
            )
            if (state.previewModel != null) {
                WorkoutTextButton(text = stringResource(R.string.exercises_remove_image), onClick = viewModel::removeImage)
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.exercises_name_label)) },
            placeholder = { Text(stringResource(R.string.exercises_name_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Type ---
        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        SectionHeader(stringResource(R.string.exercises_type))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WorkoutTheme.spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ExerciseKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { viewModel.setKind(kind) },
                    label = { Text(kind.label) },
                )
            }
        }
        Spacer(Modifier.height(WorkoutTheme.spacing.sm))
        Text(
            text = stringResource(state.kind.hintRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // --- Détails ---
        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        SectionHeader(stringResource(R.string.exercises_details))
        OptionDropdown(
            label = stringResource(R.string.exercises_category),
            selected = state.category,
            options = ExerciseCategory.entries,
            optionLabel = { it.label },
            onSelect = viewModel::setCategory,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.md))
        OptionDropdown(
            label = stringResource(R.string.exercises_primary_muscle),
            selected = state.primaryMuscle,
            options = Muscle.entries,
            optionLabel = { it.label },
            onSelect = viewModel::setPrimaryMuscle,
        )
        Spacer(Modifier.height(WorkoutTheme.spacing.md))
        OptionDropdown(
            label = stringResource(R.string.exercises_equipment),
            selected = state.equipment,
            options = Equipment.entries,
            optionLabel = { it.label },
            onSelect = viewModel::setEquipment,
        )

        // --- Charge ---
        if (state.kind.hasWeight) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader(stringResource(R.string.exercises_machine))
            val unit = LocalWeightUnit.current
            // En livres, 1 kg et 1,25 kg donnent le même pas rond (2,5 lb) : un seul cran par valeur affichée.
            val steps = vm.WEIGHT_STEPS.distinctBy { unit.step(it) }
            val index = steps.indexOfFirst { unit.step(it) == unit.step(state.weightStepKg) }
                .takeIf { it >= 0 } ?: steps.indexOf(2.5)
            StepperRow(
                label = stringResource(R.string.exercises_weight_step),
                // Le pas est stocké en kilos ; en livres on montre le pas rond réellement appliqué.
                value = "${formatNumber(unit.step(steps[index]))} ${unit.symbol}",
                onDecrement = { viewModel.setWeightStep(steps[index - 1]) },
                onIncrement = { viewModel.setWeightStep(steps[index + 1]) },
                canDecrement = index > 0,
                canIncrement = index < steps.lastIndex,
            )
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))
        Text(
            text = stringResource(R.string.exercises_settings_in_workout),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.imageError) {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            Text(stringResource(R.string.exercises_image_error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        WorkoutPrimaryButton(
            text = stringResource(R.string.exercises_save),
            onClick = { scope.launch { if (viewModel.save()) onDone() } },
            enabled = state.canSave,
            icon = Icons.Rounded.Check,
        )

        if (state.isExisting) {
            Spacer(Modifier.height(WorkoutTheme.spacing.sm))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WorkoutTextButton(
                    text = stringResource(R.string.exercises_delete),
                    onClick = { scope.launch { usedBy = viewModel.workoutsUsingIt() } },
                    icon = Icons.Rounded.Delete,
                )
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
    }

    usedBy?.let { workouts ->
        AlertDialog(
            onDismissRequest = { usedBy = null },
            title = { Text(stringResource(R.string.exercises_delete_title, state.name.trim())) },
            text = {
                val usedByText = stringResource(R.string.exercises_delete_used_by, workouts.joinToString(", "))
                val historyKept = stringResource(R.string.exercises_delete_history_kept)
                Text(
                    buildString {
                        if (workouts.isNotEmpty()) {
                            append(usedByText).append("\n\n")
                        }
                        append(historyKept)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        usedBy = null
                        scope.launch {
                            viewModel.delete()
                            onDeleted()
                        }
                    },
                ) { Text(stringResource(R.string.exercises_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { usedBy = null }) { Text(stringResource(R.string.exercises_cancel)) } },
        )
    }
}


/** L'explication affichée sous le type choisi. */
private val ExerciseKind.hintRes: Int
    get() = when (this) {
        ExerciseKind.WEIGHTED_REPS -> R.string.exercises_kind_weighted_hint
        ExerciseKind.REPS_ONLY -> R.string.exercises_kind_reps_only_hint
        ExerciseKind.TIMED -> R.string.exercises_kind_timed_hint
        ExerciseKind.WEIGHTED_TIMED -> R.string.exercises_kind_weighted_timed_hint
        ExerciseKind.DISTANCE -> R.string.exercises_kind_distance_hint
        ExerciseKind.WEIGHTED_DISTANCE -> R.string.exercises_kind_weighted_distance_hint
        ExerciseKind.TIMED_DISTANCE -> R.string.exercises_kind_timed_distance_hint
    }

/** Un choix facultatif dans une liste : « Non renseigné » en tête pour revenir à rien. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionDropdown(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { optionLabel(it) } ?: stringResource(R.string.exercises_not_set),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercises_not_set)) },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

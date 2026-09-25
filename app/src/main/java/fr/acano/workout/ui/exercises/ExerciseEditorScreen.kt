package fr.acano.workout.ui.exercises

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.common.formatNumber
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
            text = if (state.isExisting) "Modifier l'exercice" else "Nouvel exercice",
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
                    .clickable(onClickLabel = "Choisir une image", onClick = ::launchPicker),
            )
        }
        Spacer(Modifier.height(WorkoutTheme.spacing.md))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            WorkoutTonalButton(
                text = if (state.previewModel == null) "Choisir une image" else "Changer l'image",
                onClick = ::launchPicker,
                icon = Icons.Rounded.AddPhotoAlternate,
            )
            if (state.previewModel != null) {
                WorkoutTextButton(text = "Retirer", onClick = viewModel::removeImage)
            }
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xl))

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text("Nom") },
            placeholder = { Text("Ex. : Développé incliné") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Type ---
        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
        SectionHeader("Type")
        val kinds = listOf(
            ExerciseKind.WEIGHTED_REPS to "Charge",
            ExerciseKind.REPS_ONLY to "Reps seules",
            ExerciseKind.TIMED to "Durée",
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            kinds.forEachIndexed { index, (kind, label) ->
                SegmentedButton(
                    selected = state.kind == kind,
                    onClick = { viewModel.setKind(kind) },
                    shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                ) { Text(label, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(WorkoutTheme.spacing.sm))
        Text(
            text = when (state.kind) {
                ExerciseKind.WEIGHTED_REPS -> "Une charge et des répétitions, comme sur une machine."
                ExerciseKind.REPS_ONLY -> "Des répétitions au poids du corps, sans charge à noter."
                ExerciseKind.TIMED -> "Un effort chronométré, comme la planche ou le vélo."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // --- Charge ---
        if (state.kind == ExerciseKind.WEIGHTED_REPS) {
            Spacer(Modifier.height(WorkoutTheme.spacing.xxl))
            SectionHeader("Machine")
            val unit = LocalWeightUnit.current
            // En livres, 1 kg et 1,25 kg donnent le même pas rond (2,5 lb) : un seul cran par valeur affichée.
            val steps = vm.WEIGHT_STEPS.distinctBy { unit.step(it) }
            val index = steps.indexOfFirst { unit.step(it) == unit.step(state.weightStepKg) }
                .takeIf { it >= 0 } ?: steps.indexOf(2.5)
            StepperRow(
                label = "Pas de charge",
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
            text = "Les séries, les répétitions ou la durée et le repos se règlent dans chaque entraînement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.error?.let {
            Spacer(Modifier.height(WorkoutTheme.spacing.lg))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(WorkoutTheme.spacing.xxl))

        WorkoutPrimaryButton(
            text = "ENREGISTRER",
            onClick = { scope.launch { if (viewModel.save()) onDone() } },
            enabled = state.canSave,
            icon = Icons.Rounded.Check,
        )

        if (state.isExisting) {
            Spacer(Modifier.height(WorkoutTheme.spacing.sm))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WorkoutTextButton(
                    text = "Supprimer l'exercice",
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
            title = { Text("Supprimer « ${state.name.trim()} » ?") },
            text = {
                Text(
                    buildString {
                        if (workouts.isNotEmpty()) {
                            append("Il sera retiré de : ${workouts.joinToString(", ")}.\n\n")
                        }
                        append("Les séances déjà faites restent dans l'historique.")
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
                ) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { usedBy = null }) { Text("Annuler") } },
        )
    }
}


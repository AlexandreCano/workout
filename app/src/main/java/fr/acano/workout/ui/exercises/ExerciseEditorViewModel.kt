package fr.acano.workout.ui.exercises

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.ExerciseImageStore
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.ExerciseKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseEditorUiState(
    val isLoading: Boolean = true,
    val isExisting: Boolean = false,
    val name: String = "",
    val kind: ExerciseKind = ExerciseKind.WEIGHTED_REPS,
    val weightStepKg: Double = 2.5,
    /** Image déjà enregistrée pour cet exercice. */
    val savedImagePath: String? = null,
    /** Image choisie dans la galerie, pas encore copiée : elle ne l'est qu'à l'enregistrement. */
    val pickedImage: String? = null,
    val imageRemoved: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving

    /** Ce que l'aperçu doit montrer : la nouvelle image, sinon l'actuelle, sinon rien. */
    val previewModel: String?
        get() = pickedImage ?: savedImagePath?.takeUnless { imageRemoved }?.let { "file://$it" }
}

/**
 * Création et modification d'un exercice de l'utilisateur.
 *
 * Un exercice ne décrit que le mouvement : nom, type, image et pas de charge.
 * Séries, répétitions et repos se règlent dans chaque entraînement.
 *
 * Comme pour les entraînements, rien n'est écrit avant l'enregistrement — pas
 * même l'image, copiée seulement à ce moment-là : abandonner l'écran ne laisse
 * aucun fichier orphelin.
 */
class ExerciseEditorViewModel(
    private val repository: WorkoutRepository,
    private val images: ExerciseImageStore,
    /** Null pour un nouvel exercice. */
    private val exerciseId: String?,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseEditorUiState())
    val state: StateFlow<ExerciseEditorUiState> = _state.asStateFlow()

    private var existing: ExerciseEntity? = null

    init {
        viewModelScope.launch {
            // Seuls les exercices de l'utilisateur se modifient : ceux de l'application sont ignorés.
            val exercise = exerciseId?.let { repository.exercise(it) }?.takeIf { it.isCustom && !it.isArchived }
            existing = exercise
            _state.value = if (exercise == null) {
                ExerciseEditorUiState(isLoading = false)
            } else {
                ExerciseEditorUiState(
                    isLoading = false,
                    isExisting = true,
                    name = exercise.name,
                    kind = exercise.kind,
                    weightStepKg = exercise.weightStepKg,
                    savedImagePath = exercise.imagePath,
                )
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name.take(MAX_NAME_LENGTH)) }

    fun setKind(kind: ExerciseKind) = _state.update { it.copy(kind = kind) }

    fun setWeightStep(kg: Double) = _state.update { it.copy(weightStepKg = kg) }

    fun pickImage(uri: Uri) = _state.update { it.copy(pickedImage = uri.toString(), imageRemoved = false, error = null) }

    fun removeImage() = _state.update { it.copy(pickedImage = null, imageRemoved = true) }

    /** Enregistre et renvoie vrai en cas de succès ; sinon l'erreur est exposée dans l'état. */
    suspend fun save(): Boolean {
        val state = _state.value
        if (!state.canSave) return false
        _state.update { it.copy(isSaving = true, error = null) }

        val id = existing?.id ?: "custom_${now()}"
        val newImage = state.pickedImage?.let { picked ->
            runCatching { images.import(picked.toUri(), id) }.getOrElse {
                _state.update { it.copy(isSaving = false, error = "Impossible de lire cette image.") }
                return false
            }
        }
        val imagePath = when {
            newImage != null -> newImage
            state.imageRemoved -> null
            else -> state.savedImagePath
        }
        repository.saveCustomExercise(
            ExerciseEntity(
                id = id,
                name = state.name,
                kind = state.kind,
                weightStepKg = state.weightStepKg,
                imagePath = imagePath,
                isCustom = true,
            ),
        )
        // L'ancienne copie n'est effacée qu'une fois la nouvelle référencée en base.
        if (imagePath != state.savedImagePath) images.delete(state.savedImagePath)
        _state.update { it.copy(isSaving = false) }
        return true
    }

    /** Entraînements dont l'exercice sera retiré, à annoncer avant de confirmer. */
    suspend fun workoutsUsingIt(): List<String> =
        existing?.let { repository.workoutsUsing(it.id) }.orEmpty()

    suspend fun delete() {
        val exercise = existing ?: return
        repository.deleteCustomExercise(exercise.id)
        images.delete(exercise.imagePath)
    }

    companion object {
        const val MAX_NAME_LENGTH = 40

        /** Les incréments de charge qu'on trouve sur les machines et les disques. */
        val WEIGHT_STEPS = listOf(0.5, 1.0, 1.25, 2.5, 5.0, 10.0)
    }
}

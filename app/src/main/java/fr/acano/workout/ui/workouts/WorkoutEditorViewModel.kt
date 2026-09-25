package fr.acano.workout.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.PlannedStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Un exercice de l'entraînement en cours d'édition, avec ses réglages pour cet
 * entraînement. [key] identifie la ligne dans la liste réordonnable,
 * indépendamment de sa position.
 */
data class EditorStep(
    val key: Long,
    val exercise: ExerciseEntity,
    val step: PlannedStep,
) {
    val isTimed: Boolean get() = exercise.kind == ExerciseKind.TIMED
}

data class WorkoutEditorUiState(
    val isLoading: Boolean = true,
    /** Faux en création : il n'y a alors rien à supprimer. */
    val isExisting: Boolean = false,
    val name: String = "",
    val steps: List<EditorStep> = emptyList(),
    /** Tout le catalogue, dans l'ordre du programme, pour le sélecteur d'exercices. */
    val catalogue: List<ExerciseEntity> = emptyList(),
    /** L'entraînement tel qu'il était à l'ouverture, pour savoir s'il reste des modifications à enregistrer. */
    val savedName: String = "",
    val savedSteps: List<PlannedStep> = emptyList(),
) {
    val hasUnsavedChanges: Boolean
        get() = !isLoading && (name.trim() != savedName.trim() || steps.map { it.step } != savedSteps)

    val canSave: Boolean get() = name.isNotBlank() && steps.isNotEmpty()
    val selectedIds: Set<String> get() = steps.mapTo(mutableSetOf()) { it.exercise.id }
}

/**
 * Création et modification d'un entraînement personnalisé.
 *
 * Tout est édité localement et n'est écrit en base qu'à l'enregistrement :
 * quitter l'écran sans enregistrer ne laisse aucune trace.
 */
class WorkoutEditorViewModel(
    private val repository: WorkoutRepository,
    /** Null pour un nouvel entraînement. */
    private val workoutId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutEditorUiState())
    val state: StateFlow<WorkoutEditorUiState> = _state.asStateFlow()

    private var nextKey = 0L

    /** Dernier exercice retiré et sa place, pour pouvoir l'annuler. */
    private var lastRemoved: Pair<Int, EditorStep>? = null

    init {
        viewModelScope.launch {
            val exercises = repository.observeCatalogue().first()
            val order = Program.exercises.map { it.id }
            val catalogue = exercises.sortedWith(
                compareBy<ExerciseEntity> { order.indexOf(it.id).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() },
            )
            val byId = catalogue.associateBy { it.id }

            val existing = workoutId?.let { repository.customWorkout(it) }
            _state.value = WorkoutEditorUiState(
                isLoading = false,
                isExisting = existing != null,
                name = existing?.workout?.name.orEmpty(),
                steps = existing?.orderedExercises.orEmpty().mapNotNull { row ->
                    byId[row.exerciseId]?.let {
                        EditorStep(
                            key = nextKey++,
                            exercise = it,
                            step = PlannedStep(
                                exerciseId = row.exerciseId,
                                plannedSets = row.plannedSets,
                                targetRepsMin = row.targetRepsMin,
                                targetRepsMax = row.targetRepsMax,
                                targetDurationSeconds = row.targetDurationSeconds,
                                restSeconds = row.restSeconds,
                            ).completedFor(it.kind),
                        )
                    }
                },
                catalogue = catalogue,
            ).let { loaded -> loaded.copy(savedName = loaded.name, savedSteps = loaded.steps.map { it.step }) }
        }
    }

    fun setName(name: String) {
        _state.update { it.copy(name = name.take(MAX_NAME_LENGTH)) }
    }

    /** Ajoute l'exercice en fin de liste, ou le retire s'il y est déjà. */
    fun toggleExercise(exercise: ExerciseEntity) {
        _state.update { state ->
            val present = state.steps.any { it.exercise.id == exercise.id }
            state.copy(
                steps = if (present) {
                    state.steps.filterNot { it.exercise.id == exercise.id }
                } else {
                    state.steps + EditorStep(nextKey++, exercise, Program.defaultStepFor(exercise.id, exercise.kind))
                },
            )
        }
    }

    /** Retire l'exercice et renvoie son nom, pour le message « Retiré : … · Annuler ». */
    fun remove(key: Long): String? {
        val steps = _state.value.steps
        val index = steps.indexOfFirst { it.key == key }
        if (index < 0) return null
        lastRemoved = index to steps[index]
        _state.update { state -> state.copy(steps = state.steps.filterNot { it.key == key }) }
        return steps[index].exercise.name
    }

    /** Remet le dernier exercice retiré à sa place d'origine. */
    fun undoRemove() {
        val (index, step) = lastRemoved ?: return
        lastRemoved = null
        _state.update { state ->
            val steps = state.steps.toMutableList()
            steps.add(index.coerceAtMost(steps.size), step)
            state.copy(steps = steps)
        }
    }

    fun move(from: Int, to: Int) {
        _state.update { state ->
            val steps = state.steps.toMutableList()
            if (from !in steps.indices || to !in steps.indices) return@update state
            steps.add(to, steps.removeAt(from))
            state.copy(steps = steps)
        }
    }

    fun setPlannedSets(key: Long, sets: Int) {
        updateStep(key) { it.copy(plannedSets = sets.coerceIn(MIN_SETS, MAX_SETS)) }
    }

    /** Les bornes restent ordonnées : monter le minimum au-dessus du maximum pousse le maximum. */
    fun setRepsMin(key: Long, reps: Int) {
        updateStep(key) { step ->
            val min = reps.coerceIn(MIN_REPS, MAX_REPS)
            step.copy(targetRepsMin = min, targetRepsMax = maxOf(min, step.targetRepsMax ?: min))
        }
    }

    fun setRepsMax(key: Long, reps: Int) {
        updateStep(key) { step ->
            val max = reps.coerceIn(MIN_REPS, MAX_REPS)
            step.copy(targetRepsMax = max, targetRepsMin = minOf(max, step.targetRepsMin ?: max))
        }
    }

    fun setDuration(key: Long, seconds: Int) {
        updateStep(key) { it.copy(targetDurationSeconds = seconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)) }
    }

    fun setRest(key: Long, seconds: Int) {
        updateStep(key) { it.copy(restSeconds = seconds.coerceIn(0, MAX_REST_SECONDS)) }
    }

    private fun updateStep(key: Long, transform: (PlannedStep) -> PlannedStep) {
        _state.update { state ->
            state.copy(steps = state.steps.map { if (it.key == key) it.copy(step = transform(it.step)) else it })
        }
    }

    /** Enregistre et renvoie vrai si l'entraînement était valide. */
    suspend fun save(): Boolean {
        val state = _state.value
        if (!state.canSave) return false
        repository.saveCustomWorkout(
            workoutId = workoutId.takeIf { state.isExisting },
            name = state.name,
            steps = state.steps.map { it.step },
        )
        return true
    }

    suspend fun delete() {
        workoutId?.let { repository.deleteCustomWorkout(it) }
    }

    companion object {
        const val MIN_SETS = 1
        const val MAX_SETS = 10
        const val MIN_REPS = 1
        const val MAX_REPS = 50
        const val MIN_DURATION_SECONDS = 15
        const val MAX_DURATION_SECONDS = 60 * 60
        const val MAX_REST_SECONDS = 10 * 60
        const val MAX_NAME_LENGTH = 40
    }
}

/**
 * Garantit qu'une étape a la cible propre à son type : une fourchette de
 * répétitions, ou une durée. Utile si le type d'un exercice a changé depuis
 * que l'étape a été créée.
 */
private fun PlannedStep.completedFor(kind: ExerciseKind): PlannedStep {
    val defaults = Program.defaultStepFor(exerciseId, kind)
    return if (kind == ExerciseKind.TIMED) {
        copy(targetRepsMin = null, targetRepsMax = null,
            targetDurationSeconds = targetDurationSeconds ?: defaults.targetDurationSeconds)
    } else {
        copy(targetDurationSeconds = null,
            targetRepsMin = targetRepsMin ?: defaults.targetRepsMin,
            targetRepsMax = targetRepsMax ?: defaults.targetRepsMax)
    }
}

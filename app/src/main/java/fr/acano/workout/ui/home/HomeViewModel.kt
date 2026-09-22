package fr.acano.workout.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.StepState
import fr.acano.workout.domain.WorkoutProgression
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ResumableSession(
    val sessionId: Long,
    val type: WorkoutType,
    val exerciseName: String,
    val setNumber: Int,
    val plannedSets: Int,
    val stepIndex: Int,
    val totalSteps: Int,
)

data class ProgressionLine(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    /** +1 en progression, -1 en baisse, 0 si la charge n'a pas bougé. */
    val trend: Int,
)

data class HomeUiState(
    val stars: Int = 0,
    val upperCount: Int = 0,
    val lowerCount: Int = 0,
    val lastSessionType: WorkoutType? = null,
    val lastSessionAt: Long? = null,
    val resumable: ResumableSession? = null,
    val progression: List<ProgressionLine> = emptyList(),
    /** Nombre d'étapes de chaque séance, pour annoncer ce qui attend avant de lancer. */
    val stepsPerSession: Map<WorkoutType, Int> =
        WorkoutType.entries.associateWith { Program.planFor(it).size },
) {
    /**
     * Séance suggérée : celle qu'on n'a pas faite la dernière fois. Donne à
     * l'accueil une hiérarchie naturelle sans imposer quoi que ce soit.
     */
    val suggestedType: WorkoutType
        get() = when (lastSessionType) {
            WorkoutType.UPPER_BODY -> WorkoutType.LOWER_BODY
            else -> WorkoutType.UPPER_BODY
        }
}

class HomeViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repository.observeStarCount(),
        repository.observeStarCount(WorkoutType.UPPER_BODY),
        repository.observeStarCount(WorkoutType.LOWER_BODY),
        repository.observeLastFinishedSession(),
        combine(
            repository.observeActiveSession(),
            repository.observeExercises(),
            repository.observeRecentWeightedSets(),
        ) { active, exercises, recentSets -> Triple(active, exercises, recentSets) },
    ) { stars, upper, lower, last, (active, exercises, recentSets) ->
        val catalogue = exercises.associateBy { it.id }
        HomeUiState(
            stars = stars,
            upperCount = upper,
            lowerCount = lower,
            lastSessionType = last?.type,
            lastSessionAt = last?.startedAt,
            resumable = active?.let { session ->
                val steps = session.orderedExercises
                val progress = WorkoutProgression.compute(
                    steps.map { StepState(it.exerciseSession.plannedSets, it.sets.size) },
                )
                val index = progress.currentStepIndex ?: 0
                val current = steps.getOrNull(index)
                ResumableSession(
                    sessionId = session.session.id,
                    type = session.session.type,
                    exerciseName = current
                        ?.let { catalogue[it.exerciseSession.exerciseId]?.name }
                        .orEmpty(),
                    setNumber = progress.currentSetNumber,
                    plannedSets = current?.exerciseSession?.plannedSets ?: 0,
                    stepIndex = index + 1,
                    totalSteps = steps.size,
                )
            },
            progression = buildProgression(catalogue, recentSets),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Pour chaque exercice chargé : la dernière charge utilisée, comparée à la charge
     * différente qui la précédait. Limité aux quelques exercices les plus récents,
     * l'accueil n'étant pas un tableau de bord.
     */
    private fun buildProgression(
        catalogue: Map<String, ExerciseEntity>,
        recentSets: List<fr.acano.workout.data.db.entity.SetResultEntity>,
    ): List<ProgressionLine> =
        recentSets
            .groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, sets) ->
                val exercise = catalogue[exerciseId]
                    ?.takeIf { it.kind == ExerciseKind.WEIGHTED_REPS }
                    ?: return@mapNotNull null
                val weights = sets.mapNotNull { it.weightKg }
                val latest = weights.firstOrNull() ?: return@mapNotNull null
                val previous = weights.firstOrNull { it != latest }
                ProgressionLine(
                    exerciseId = exerciseId,
                    exerciseName = exercise.name,
                    weightKg = latest,
                    trend = when {
                        previous == null -> 0
                        latest > previous -> 1
                        latest < previous -> -1
                        else -> 0
                    },
                )
            }
            .sortedByDescending { line ->
                recentSets.firstOrNull { it.exerciseId == line.exerciseId }?.completedAt ?: 0L
            }
            .take(4)

    suspend fun startSession(type: WorkoutType): Long = repository.startSession(type)
}

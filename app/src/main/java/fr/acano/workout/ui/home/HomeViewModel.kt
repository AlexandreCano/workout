package fr.acano.workout.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.title
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.StepState
import fr.acano.workout.domain.WorkoutProgression
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ResumableSession(
    val sessionId: Long,
    val title: String,
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

/** Un entraînement, tel qu'il se présente sur l'accueil. */
data class WorkoutLaunch(
    val id: Long,
    val name: String,
    val stepCount: Int,
    /** Nombre de séances terminées avec cet entraînement. */
    val sessionCount: Int = 0,
    val lastDoneAt: Long? = null,
)

data class HomeUiState(
    val stars: Int = 0,
    val workouts: List<WorkoutLaunch> = emptyList(),
    val resumable: ResumableSession? = null,
    val progression: List<ProgressionLine> = emptyList(),
) {
    /**
     * Entraînement suggéré : celui qu'on n'a pas fait depuis le plus longtemps,
     * un entraînement jamais fait passant en premier. Avec deux entraînements,
     * c'est « celui qu'on n'a pas fait la dernière fois ». Donne à l'accueil une
     * hiérarchie naturelle sans rien imposer.
     */
    val suggestedWorkoutId: Long?
        get() = workouts.minByOrNull { it.lastDoneAt ?: Long.MIN_VALUE }?.id
}

class HomeViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val workouts = combine(
        repository.observeCustomWorkouts(),
        repository.observeWorkoutStats(),
    ) { workouts, stats ->
        workouts.map {
            val stat = stats[it.workout.id]
            WorkoutLaunch(
                id = it.workout.id,
                name = it.workout.name,
                stepCount = it.exercises.size,
                sessionCount = stat?.sessionCount ?: 0,
                lastDoneAt = stat?.lastDoneAt,
            )
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        repository.observeStarCount(),
        workouts,
        combine(
            repository.observeActiveSession(),
            repository.observeExercises(),
            repository.observeRecentWeightedSets(),
        ) { active, exercises, recentSets -> Triple(active, exercises, recentSets) },
    ) { stars, workouts, (active, exercises, recentSets) ->
        val catalogue = exercises.associateBy { it.id }
        HomeUiState(
            stars = stars,
            workouts = workouts,
            resumable = active?.let { session ->
                val steps = session.orderedExercises
                val progress = WorkoutProgression.compute(
                    steps.map { StepState(it.exerciseSession.plannedSets, it.sets.size) },
                )
                val index = progress.currentStepIndex ?: 0
                val current = steps.getOrNull(index)
                ResumableSession(
                    sessionId = session.session.id,
                    title = session.session.title(),
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


    suspend fun startSession(workoutId: Long): Long = repository.startSession(workoutId)
}

/**
 * Pour chaque exercice chargé : la charge de la dernière séance terminée,
 * comparée à celle de la séance précédente. La charge d'une séance est la plus
 * lourde utilisée ce jour-là — changer de poids d'une série à l'autre au sein
 * d'une même séance n'est donc pas une « progression ».
 *
 * [recentSets] ne contient que des séances terminées, de la plus récente à la
 * plus ancienne. Limité aux quelques exercices les plus récents : l'accueil
 * n'est pas un tableau de bord.
 */
internal fun buildProgression(
    catalogue: Map<String, ExerciseEntity>,
    recentSets: List<SetResultEntity>,
): List<ProgressionLine> =
    recentSets
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, sets) ->
            val exercise = catalogue[exerciseId]
                // Un exercice supprimé ne fait plus partie de la progression affichée.
                ?.takeIf { it.kind == ExerciseKind.WEIGHTED_REPS && !it.isArchived }
                ?: return@mapNotNull null
            // Une étape de séance par séance : grouper par étape, c'est grouper par séance.
            // groupBy conserve l'ordre d'apparition, donc la séance la plus récente d'abord.
            val perSession = sets
                .groupBy { it.exerciseSessionId }
                .values
                .map { session -> session.mapNotNull { it.weightKg }.max() }
            val latest = perSession.firstOrNull() ?: return@mapNotNull null
            val previous = perSession.getOrNull(1)
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

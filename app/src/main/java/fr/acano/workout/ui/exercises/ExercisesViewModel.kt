package fr.acano.workout.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.Program
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ExerciseRow(
    val exercise: ExerciseEntity,
    val lastWeightKg: Double?,
    val lastPerformedAt: Long?,
)

class ExercisesViewModel(repository: WorkoutRepository) : ViewModel() {

    val exercises: StateFlow<List<ExerciseRow>> = combine(
        repository.observeExercises(),
        repository.observeRecentWeightedSets(),
    ) { exercises, recentSets ->
        val byExercise = recentSets.groupBy { it.exerciseId }
        exercises
            .filter { it.id != Program.BIKE }
            .sortedBy { Program.exercises.indexOfFirst { e -> e.id == it.id } }
            .map { exercise ->
                val sets = byExercise[exercise.id].orEmpty()
                ExerciseRow(
                    exercise = exercise,
                    lastWeightKg = sets.firstNotNullOfOrNull { it.weightKg },
                    lastPerformedAt = sets.firstOrNull()?.completedAt,
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Une séance passée, pour l'historique d'un exercice : « 18 sept · 42,5 kg · 11/10/10/9 ». */
data class ExerciseHistoryEntry(
    val date: Long,
    val weightKg: Double?,
    val repetitions: List<Int>,
    val durations: List<Int>,
)

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val history: List<ExerciseHistoryEntry> = emptyList(),
)

class ExerciseDetailViewModel(
    repository: WorkoutRepository,
    exerciseId: String,
) : ViewModel() {

    val state: StateFlow<ExerciseDetailUiState> = combine(
        repository.observeExercise(exerciseId),
        repository.observeSetsForExercise(exerciseId).map(::groupByDay),
    ) { exercise, history ->
        ExerciseDetailUiState(exercise = exercise, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    /** Les séries sont regroupées par séance, en s'appuyant sur l'identifiant d'exercice de séance. */
    private fun groupByDay(sets: List<SetResultEntity>): List<ExerciseHistoryEntry> =
        sets.groupBy { it.exerciseSessionId }
            .map { (_, rows) ->
                val ordered = rows.sortedBy { it.setNumber }
                ExerciseHistoryEntry(
                    date = ordered.first().completedAt,
                    weightKg = ordered.firstNotNullOfOrNull { it.weightKg },
                    repetitions = ordered.mapNotNull { it.repetitions },
                    durations = ordered.mapNotNull { it.durationSeconds },
                )
            }
            .sortedByDescending { it.date }
}

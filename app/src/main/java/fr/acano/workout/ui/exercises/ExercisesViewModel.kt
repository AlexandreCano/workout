package fr.acano.workout.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.bestSetIndex
import fr.acano.workout.domain.latestSessionBest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer

data class ExerciseRow(
    val exercise: ExerciseEntity,
    /** Charge la plus lourde de la dernière séance terminée : la même règle partout. */
    val lastWeightKg: Double?,
    /** Nombre de séances terminées où l'exercice a été fait. */
    val sessionCount: Int = 0,
    val lastDoneAt: Long? = null,
)

/** Le catalogue en deux parties : ce que fournit l'application, et ce que l'utilisateur a ajouté. */
data class ExercisesUiState(
    val query: String = "",
    val builtIn: List<ExerciseRow> = emptyList(),
    val custom: List<ExerciseRow> = emptyList(),
    /** Vrai si l'utilisateur a au moins un exercice à lui, recherche ou pas. */
    val hasCustom: Boolean = false,
)

class ExercisesViewModel(repository: WorkoutRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<ExercisesUiState> = combine(
        repository.observeCatalogue(),
        repository.observeRecentWeightedSets(),
        repository.observeExerciseUsage(),
        query,
    ) { exercises, recentSets, usage, query ->
        val byExercise = recentSets.groupBy { it.exerciseId }
        fun row(exercise: ExerciseEntity) = ExerciseRow(
            exercise = exercise,
            lastWeightKg = latestSessionBest(byExercise[exercise.id].orEmpty()),
            sessionCount = usage[exercise.id]?.sessionCount ?: 0,
            lastDoneAt = usage[exercise.id]?.lastDoneAt,
        )
        val needle = query.normalized()
        val (custom, builtIn) = exercises
            .filter { it.id != Program.BIKE }
            .partition { it.isCustom }
        ExercisesUiState(
            query = query,
            builtIn = builtIn
                .filter { needle.isEmpty() || needle in it.name.normalized() }
                .sortedBy { Program.exercises.indexOfFirst { e -> e.id == it.id } }
                .map(::row),
            custom = custom
                .filter { needle.isEmpty() || needle in it.name.normalized() }
                .sortedBy { it.name.lowercase() }
                .map(::row),
            hasCustom = custom.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesUiState())

    fun setQuery(value: String) {
        query.value = value.take(40)
    }
}

/** Recherche tolérante : « presse », « Presse » et « préssé » se retrouvent. */
private fun String.normalized(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/** Une séance passée, dans l'historique d'un exercice : sa date et ses séries. */
data class ExerciseHistoryEntry(
    val date: Long,
    val sets: List<SetResultEntity>,
    val bestSetIndex: Int?,
    /** Charge la plus lourde de la séance ; null pour un exercice sans charge. */
    val bestWeightKg: Double?,
)

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    /** De la séance la plus récente à la plus ancienne. */
    val history: List<ExerciseHistoryEntry> = emptyList(),
) {
    val lastWeightKg: Double? get() = history.firstNotNullOfOrNull { it.bestWeightKg }

    /** Points de la courbe, dans l'ordre chronologique : (date, charge de la séance). */
    val chartPoints: List<Pair<Long, Double>>
        get() = history.reversed().mapNotNull { entry -> entry.bestWeightKg?.let { entry.date to it } }
}

class ExerciseDetailViewModel(
    repository: WorkoutRepository,
    exerciseId: String,
) : ViewModel() {

    val state: StateFlow<ExerciseDetailUiState> = combine(
        repository.observeExercise(exerciseId),
        repository.observeSetsForExercise(exerciseId).map(::groupBySession),
    ) { exercise, history ->
        ExerciseDetailUiState(exercise = exercise, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    /** Une étape de séance par séance : grouper par étape, c'est grouper par séance. */
    private fun groupBySession(sets: List<SetResultEntity>): List<ExerciseHistoryEntry> =
        sets.groupBy { it.exerciseSessionId }
            .map { (_, rows) ->
                val ordered = rows.sortedBy { it.setNumber }
                ExerciseHistoryEntry(
                    date = ordered.first().completedAt,
                    sets = ordered,
                    bestSetIndex = bestSetIndex(ordered),
                    bestWeightKg = ordered.mapNotNull { it.weightKg }.maxOrNull(),
                )
            }
            .sortedByDescending { it.date }
}

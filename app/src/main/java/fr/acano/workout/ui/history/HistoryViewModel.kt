package fr.acano.workout.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionSummaryRow(
    val sessionId: Long,
    val type: WorkoutType,
    val startedAt: Long,
    val durationMillis: Long,
    val exerciseNames: List<String>,
)

class HistoryViewModel(repository: WorkoutRepository) : ViewModel() {

    val sessions: StateFlow<List<SessionSummaryRow>> = combine(
        repository.observeFinishedSessions(),
        repository.observeExercises(),
    ) { sessions, exercises ->
        val catalogue = exercises.associateBy { it.id }
        sessions.map { session ->
            SessionSummaryRow(
                sessionId = session.session.id,
                type = session.session.type,
                startedAt = session.session.startedAt,
                durationMillis = (session.session.endedAt ?: session.session.startedAt) -
                    session.session.startedAt,
                exerciseNames = session.orderedExercises
                    .filter { it.sets.isNotEmpty() }
                    .mapNotNull { catalogue[it.exerciseSession.exerciseId]?.name },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Une ligne du détail d'une séance : un exercice, sa charge, et les répétitions série par série. */
data class SessionDetailLine(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double?,
    val sets: List<SetResultEntity>,
)

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val type: WorkoutType? = null,
    val startedAt: Long = 0,
    val durationMillis: Long = 0,
    val isInProgress: Boolean = false,
    val lines: List<SessionDetailLine> = emptyList(),
)

class SessionDetailViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
) : ViewModel() {

    /**
     * Clôture la séance et attribue l'étoile. Appelé dès l'affichage du récapitulatif :
     * si l'application est tuée sur cet écran, la séance reste correctement terminée
     * au lieu de réapparaître comme « en cours ».
     */
    fun finishIfNeeded() {
        viewModelScope.launch { repository.finishSession(sessionId) }
    }

    val state: StateFlow<SessionDetailUiState> = combine(
        repository.observeSession(sessionId),
        repository.observeExercises(),
    ) { session, exercises ->
        toUiState(session, exercises.associateBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    private fun toUiState(
        session: SessionWithContent?,
        catalogue: Map<String, ExerciseEntity>,
    ): SessionDetailUiState {
        if (session == null) return SessionDetailUiState(isLoading = false)
        return SessionDetailUiState(
            isLoading = false,
            type = session.session.type,
            startedAt = session.session.startedAt,
            durationMillis = (session.session.endedAt ?: System.currentTimeMillis()) -
                session.session.startedAt,
            isInProgress = session.session.endedAt == null,
            lines = session.orderedExercises
                .filter { it.sets.isNotEmpty() }
                .map { item ->
                    val sets = item.sets.sortedBy { it.setNumber }
                    SessionDetailLine(
                        exerciseId = item.exerciseSession.exerciseId,
                        exerciseName = catalogue[item.exerciseSession.exerciseId]?.name
                            ?: item.exerciseSession.exerciseId,
                        weightKg = sets.firstNotNullOfOrNull { it.weightKg },
                        sets = sets,
                    )
                },
        )
    }
}

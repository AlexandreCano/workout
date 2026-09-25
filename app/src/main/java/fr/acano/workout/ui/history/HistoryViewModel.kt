package fr.acano.workout.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.title
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.Calories
import fr.acano.workout.domain.SessionComparison
import fr.acano.workout.domain.UserProfile
import fr.acano.workout.domain.bestSetIndex
import fr.acano.workout.domain.compareWithPreviousSession
import fr.acano.workout.domain.monthGrid
import fr.acano.workout.domain.volumeKg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class SessionSummaryRow(
    val sessionId: Long,
    val title: String,
    val startedAt: Long,
    val durationMillis: Long,
    val exerciseNames: List<String>,
    /** Charge totale soulevée pendant la séance, en kilos ; 0 si rien de chargé. */
    val volumeKg: Double = 0.0,
    /** Calories estimées, ou null si aucun exercice n'est estimable ou le profil manque. */
    val kcal: Double? = null,
)

/** Le calendrier et la liste de l'historique, tels qu'ils doivent s'afficher. */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val weeks: List<List<LocalDate?>> = emptyList(),
    /** Nombre de séances terminées par jour, pour marquer le calendrier. */
    val sessionsPerDay: Map<LocalDate, Int> = emptyMap(),
    val selectedDay: LocalDate? = null,
    val canGoToPreviousMonth: Boolean = false,
    val canGoToNextMonth: Boolean = false,
    /** Séances du mois affiché, pour le résumé sous le calendrier. */
    val sessionsInMonth: Int = 0,
    /** La liste : toutes les séances, ou celles du jour choisi. */
    val sessions: List<SessionSummaryRow> = emptyList(),
    val totalSessions: Int = 0,
)

class HistoryViewModel(
    repository: WorkoutRepository,
    profile: Flow<UserProfile> = flowOf(UserProfile()),
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: () -> LocalDate = { LocalDate.now(zone) },
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today()))
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    private val allSessions: StateFlow<List<SessionSummaryRow>> = combine(
        repository.observeFinishedSessions(),
        repository.observeExercises(),
        profile,
    ) { sessions, exercises, profile ->
        val catalogue = exercises.associateBy { it.id }
        sessions.map { session ->
            SessionSummaryRow(
                sessionId = session.session.id,
                title = session.session.title(),
                startedAt = session.session.startedAt,
                durationMillis = (session.session.endedAt ?: session.session.startedAt) -
                    session.session.startedAt,
                exerciseNames = session.orderedExercises
                    .filter { it.sets.isNotEmpty() }
                    .mapNotNull { catalogue[it.exerciseSession.exerciseId]?.name },
                volumeKg = session.orderedExercises.sumOf { volumeKg(it.sets) },
                kcal = sessionKcal(session, profile, today().year),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<HistoryUiState> = combine(allSessions, month, selectedDay) { sessions, month, selected ->
        val perDay = sessions.groupingBy { it.day(zone) }.eachCount()
        val currentMonth = YearMonth.from(today())
        val firstMonth = sessions.minOfOrNull { YearMonth.from(it.day(zone)) } ?: currentMonth
        HistoryUiState(
            isLoading = false,
            month = month,
            weeks = monthGrid(month),
            sessionsPerDay = perDay,
            selectedDay = selected,
            canGoToPreviousMonth = month > firstMonth,
            canGoToNextMonth = month < currentMonth,
            sessionsInMonth = perDay.filterKeys { YearMonth.from(it) == month }.values.sum(),
            sessions = if (selected == null) sessions else sessions.filter { it.day(zone) == selected },
            totalSessions = sessions.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun previousMonth() = changeMonth(-1)

    fun nextMonth() = changeMonth(1)

    private fun changeMonth(delta: Long) {
        val target = month.value.plusMonths(delta)
        val current = YearMonth.from(today())
        month.value = if (target > current) current else target
        // Un jour choisi dans un autre mois n'a plus rien à filtrer à l'écran.
        selectedDay.value = null
    }

    /** Choisit un jour, ou le désélectionne s'il l'était déjà. Un jour sans séance est ignoré. */
    fun toggleDay(day: LocalDate) {
        if (state.value.sessionsPerDay[day] == null) return
        selectedDay.value = if (selectedDay.value == day) null else day
    }

    fun clearSelection() {
        selectedDay.value = null
    }
}

/** Calories estimées d'une séance terminée, ou null si rien n'est estimable. */
private fun sessionKcal(session: SessionWithContent, profile: UserProfile, year: Int): Double? {
    val done = session.orderedExercises.filter { it.sets.isNotEmpty() }
    return Calories.perExercise(session.session.startedAt, done.map { it.exerciseSession.exerciseId to it.sets }, profile, year)
        .filterNotNull()
        .takeIf { it.isNotEmpty() }
        ?.sum()
}

private fun SessionSummaryRow.day(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()

/** Une ligne du détail d'une séance : un exercice et ses séries, telles qu'elles ont été faites. */
data class SessionDetailLine(
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<SetResultEntity>,
    /** Pour la vignette ; null si l'exercice a disparu du catalogue. */
    val exercise: ExerciseEntity? = null,
    /** Calories estimées pour cet exercice ; null s'il n'est pas estimable. */
    val kcal: Double? = null,
    /** Série mise en avant, ou null si aucune ne se distingue. */
    val bestSetIndex: Int? = null,
    /** Écart avec la séance précédente ; null pour un exercice sans charge. */
    val comparison: SessionComparison? = null,
)

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val startedAt: Long = 0,
    val durationMillis: Long = 0,
    val isInProgress: Boolean = false,
    val lines: List<SessionDetailLine> = emptyList(),
    /** Vrai s'il manque le poids pour estimer les calories d'exercices qui le permettraient. */
    val needsProfileForCalories: Boolean = false,
) {
    val totalSets: Int get() = lines.sumOf { it.sets.size }

    /** Calories estimées de la séance : somme des exercices estimables, ou null s'il n'y en a aucun. */
    val totalKcal: Double? get() = lines.mapNotNull { it.kcal }.takeIf { it.isNotEmpty() }?.sum()
    val volumeKg: Double get() = lines.sumOf { volumeKg(it.sets) }
}

class SessionDetailViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
    private val profile: Flow<UserProfile> = flowOf(UserProfile()),
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
        repository.observeRecentWeightedSets(),
        profile,
    ) { session, exercises, history, profile ->
        toUiState(session, exercises.associateBy { it.id }, history, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    private fun toUiState(
        session: SessionWithContent?,
        catalogue: Map<String, ExerciseEntity>,
        history: List<SetResultEntity>,
        profile: UserProfile,
    ): SessionDetailUiState {
        if (session == null) return SessionDetailUiState(isLoading = false)
        return SessionDetailUiState(
            isLoading = false,
            title = session.session.title(),
            startedAt = session.session.startedAt,
            durationMillis = (session.session.endedAt ?: System.currentTimeMillis()) -
                session.session.startedAt,
            isInProgress = session.session.endedAt == null,
            needsProfileForCalories = !profile.canEstimate &&
                session.orderedExercises.any { it.sets.isNotEmpty() && Calories.hasEstimate(it.exerciseSession.exerciseId) },
            lines = session.orderedExercises
                .filter { it.sets.isNotEmpty() }
                .let { done ->
                    val kcal = Calories.perExercise(
                        sessionStartedAt = session.session.startedAt,
                        steps = done.map { it.exerciseSession.exerciseId to it.sets },
                        profile = profile,
                        currentYear = LocalDate.now().year,
                    )
                    done.zip(kcal)
                }
                .map { (item, kcal) ->
                    val sets = item.sets.sortedBy { it.setNumber }
                    val exerciseId = item.exerciseSession.exerciseId
                    // Ce qui a été fait avant cette séance-ci, du plus récent au plus ancien.
                    val earlier = history.filter {
                        it.exerciseId == exerciseId &&
                            it.exerciseSessionId != item.exerciseSession.id &&
                            it.completedAt < session.session.startedAt
                    }
                    SessionDetailLine(
                        exerciseId = exerciseId,
                        exerciseName = catalogue[exerciseId]?.name ?: exerciseId,
                        sets = sets,
                        exercise = catalogue[exerciseId],
                        bestSetIndex = bestSetIndex(sets),
                        comparison = compareWithPreviousSession(sets, earlier),
                        kcal = kcal,
                    )
                },
        )
    }
}

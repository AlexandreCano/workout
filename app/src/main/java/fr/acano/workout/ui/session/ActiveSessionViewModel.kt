package fr.acano.workout.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.StepState
import fr.acano.workout.domain.WorkoutProgression
import fr.acano.workout.timer.ActiveTimer
import fr.acano.workout.timer.RestTimerService
import fr.acano.workout.timer.TimerKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActiveSessionViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
) : ViewModel() {

    /**
     * Série chronométrée en attente de validation : renseignée au lancement du chrono d'effort,
     * elle garantit qu'une série n'est enregistrée qu'une fois, même si l'état « terminé »
     * du chrono est ré-émis.
     */
    private var pendingEffortStepId: Long? = null

    val state: StateFlow<ActiveSessionUiState> = combine(
        repository.observeSession(sessionId),
        repository.observeExercises(),
        repository.observePreviousWeights(sessionId),
        ActiveTimer.state,
    ) { session, exercises, previousWeights, timer ->
        buildState(session, exercises.associateBy { it.id }, previousWeights, timer)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveSessionUiState(),
    )

    private fun buildState(
        session: SessionWithContent?,
        catalogue: Map<String, ExerciseEntity>,
        previousWeights: Map<String, Double>,
        timer: fr.acano.workout.timer.TimerState?,
    ): ActiveSessionUiState {
        if (session == null) {
            return ActiveSessionUiState(isLoading = false, sessionMissing = true)
        }
        val steps = session.orderedExercises
        val progress = WorkoutProgression.compute(
            steps.map { StepState(it.exerciseSession.plannedSets, it.sets.size) },
        )
        val currentIndex = progress.currentStepIndex
        val step = currentIndex?.let { index ->
            val item = steps[index]
            val planned = item.exerciseSession
            val exercise = catalogue[planned.exerciseId] ?: return@let null
            CurrentStep(
                exerciseSessionId = item.exerciseSession.id,
                exercise = exercise,
                setNumber = progress.currentSetNumber,
                plannedSets = item.exerciseSession.plannedSets,
                plannedWeightKg = item.exerciseSession.plannedWeightKg
                    ?: previousWeights[exercise.id],
                lastSessionWeightKg = previousWeights[exercise.id],
                setsDoneToday = item.sets.sortedBy { it.setNumber },
                targetRepsMin = planned.targetRepsMin,
                targetRepsMax = planned.targetRepsMax,
                targetDurationSeconds = planned.targetDurationSeconds,
                restSeconds = planned.restSeconds,
            )
        }
        val pendingSteps = steps
            .filter { it.sets.size < it.exerciseSession.plannedSets }
            .map { item ->
                PendingStep(
                    exerciseSessionId = item.exerciseSession.id,
                    name = catalogue[item.exerciseSession.exerciseId]?.name
                        ?: item.exerciseSession.exerciseId,
                    plannedSets = item.exerciseSession.plannedSets,
                    completedSets = item.sets.size,
                    isCurrent = item.exerciseSession.id == step?.exerciseSessionId,
                )
            }

        return ActiveSessionUiState(
            isLoading = false,
            type = session.session.type,
            startedAt = session.session.startedAt,
            progress = progress,
            step = step,
            timer = timer,
            isFinished = progress.isFinished,
            pendingSteps = pendingSteps,
        )
    }

    // --- Charge ---

    fun adjustWeight(context: Context, delta: Double) {
        val step = state.value.step ?: return
        val base = step.plannedWeightKg ?: 0.0
        setWeight(context, (base + delta).coerceAtLeast(0.0))
    }

    fun setWeight(context: Context, weightKg: Double) {
        val step = state.value.step ?: return
        viewModelScope.launch {
            repository.updatePlannedWeight(step.exerciseSessionId, weightKg.coerceAtLeast(0.0))
        }
    }

    // --- Validation des séries ---

    /** Série classique : poids + répétitions réellement effectuées. */
    fun validateRepsSet(context: Context, repetitions: Int) {
        val step = state.value.step ?: return
        viewModelScope.launch {
            repository.recordSet(
                exerciseSessionId = step.exerciseSessionId,
                exerciseId = step.exercise.id,
                setNumber = step.setNumber,
                weightKg = step.plannedWeightKg.takeIf { step.exercise.kind == ExerciseKind.WEIGHTED_REPS },
                repetitions = repetitions,
            )
            startRestIfNeeded(context, step)
        }
    }

    /** Démarre le chrono d'un exercice chronométré (vélo, planche). */
    fun startEffortTimer(context: Context) {
        val step = state.value.step ?: return
        val seconds = step.targetDurationSeconds ?: return
        pendingEffortStepId = step.exerciseSessionId
        RestTimerService.start(
            context = context,
            kind = TimerKind.EFFORT,
            label = step.exercise.name,
            durationMs = seconds * 1000L,
        )
    }

    /**
     * Appelé quand le chrono d'effort atteint zéro : la série est validée automatiquement,
     * puis la récupération démarre si l'exercice en prévoit une.
     */
    fun onEffortTimerFinished(context: Context) {
        val step = state.value.step ?: return
        if (pendingEffortStepId != step.exerciseSessionId) return
        pendingEffortStepId = null
        viewModelScope.launch {
            repository.recordSet(
                exerciseSessionId = step.exerciseSessionId,
                exerciseId = step.exercise.id,
                setNumber = step.setNumber,
                // La durée réellement chronométrée : ±30 s en cours de série la
                // font diverger de la cible, et c'est l'effort réel qu'on veut suivre.
                durationSeconds = ActiveTimer.state.value
                    ?.takeIf { it.kind == TimerKind.EFFORT }
                    ?.let { ((it.totalMs + 500) / 1000).toInt() }
                    ?: step.targetDurationSeconds,
            )
            // Pas de stop() ici : startRestIfNeeded arrête déjà le chronomètre
            // quand il n'y a pas de récupération à lancer. Enchaîner un stop et
            // un start faisait se croiser destruction et création du service.
            startRestIfNeeded(context, step)
        }
    }

    /** Valide une série chronométrée sans attendre la fin du chrono. */
    fun validateTimedSetManually(context: Context, elapsedSeconds: Int) {
        val step = state.value.step ?: return
        pendingEffortStepId = null
        viewModelScope.launch {
            repository.recordSet(
                exerciseSessionId = step.exerciseSessionId,
                exerciseId = step.exercise.id,
                setNumber = step.setNumber,
                durationSeconds = elapsedSeconds,
            )
            // Pas de stop() ici : startRestIfNeeded arrête déjà le chronomètre
            // quand il n'y a pas de récupération à lancer. Enchaîner un stop et
            // un start faisait se croiser destruction et création du service.
            startRestIfNeeded(context, step)
        }
    }

    /**
     * La récupération n'est lancée qu'entre deux séries du *même* exercice :
     * le changement de machine sert lui-même de récupération.
     */
    private fun startRestIfNeeded(context: Context, step: CurrentStep) {
        val wasLastSet = step.setNumber >= step.plannedSets
        if (wasLastSet || step.restSeconds <= 0) {
            RestTimerService.stop(context)
            return
        }
        RestTimerService.start(
            context = context,
            kind = TimerKind.REST,
            // Titre fourni par le service, dans la langue du téléphone.
            label = "",
            durationMs = step.restSeconds * 1000L,
        )
    }

    // --- Ordre des exercices ---

    /**
     * Applique l'ordre choisi sur l'écran de réorganisation.
     * Le chronomètre est arrêté : après un changement d'ordre, l'exercice courant
     * peut être un autre, et une récupération héritée du précédent n'a plus de sens.
     */
    fun applyPendingOrder(context: Context, orderedExerciseSessionIds: List<Long>) {
        val current = state.value.pendingSteps.map { it.exerciseSessionId }
        if (current == orderedExerciseSessionIds) return
        skipTimer(context)
        viewModelScope.launch {
            repository.applyPendingOrder(sessionId, orderedExerciseSessionIds)
        }
    }

    fun undoLastSet(context: Context) {
        val step = state.value.step ?: return
        viewModelScope.launch {
            RestTimerService.stop(context)
            repository.undoLastSet(step.exerciseSessionId)
        }
    }

    // --- Chronomètre ---

    fun pauseTimer(context: Context) = RestTimerService.pause(context)

    fun resumeTimer(context: Context) = RestTimerService.resume(context)

    fun addThirtySeconds(context: Context) = RestTimerService.addTime(context, 30_000L)

    fun removeThirtySeconds(context: Context) = RestTimerService.addTime(context, -30_000L)

    fun skipTimer(context: Context) {
        pendingEffortStepId = null
        RestTimerService.stop(context)
    }

    // --- Fin de séance ---

    fun finishSession(context: Context) {
        RestTimerService.stop(context)
        viewModelScope.launch { repository.finishSession(sessionId) }
    }

    fun abortSession(context: Context) {
        RestTimerService.stop(context)
        viewModelScope.launch { repository.abortSession(sessionId) }
    }
}

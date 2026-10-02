package fr.acano.workout.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.StepState
import fr.acano.workout.domain.WorkoutProgression
import fr.acano.workout.timer.ActiveTimer
import fr.acano.workout.timer.RestTimerService
import fr.acano.workout.timer.TimerKind
import kotlinx.coroutines.flow.MutableStateFlow
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

    /** Série chronométrée terminée qui attend sa distance (voir [PendingDistance]). */
    private val pendingDistance = MutableStateFlow<PendingDistance?>(null)

    private val previous = combine(
        repository.observePreviousWeights(sessionId),
        repository.observePreviousDistances(sessionId),
    ) { weights, distances -> Previous(weights, distances) }

    val state: StateFlow<ActiveSessionUiState> = combine(
        repository.observeSession(sessionId),
        repository.observeExercises(),
        previous,
        ActiveTimer.state,
        pendingDistance,
    ) { session, exercises, previous, timer, pending ->
        buildState(session, exercises.associateBy { it.id }, previous, timer)
            // Une distance en attente ne vaut que pour l'étape et la série affichées.
            .let { state ->
                state.copy(
                    pendingDistance = pending?.takeIf {
                        it.exerciseSessionId == state.step?.exerciseSessionId && it.setNumber == state.step.setNumber
                    },
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveSessionUiState(),
    )

    /** Dernières valeurs connues par exercice, hors séance en cours. */
    private data class Previous(val weights: Map<String, Double>, val distances: Map<String, Double>)

    private fun buildState(
        session: SessionWithContent?,
        catalogue: Map<String, ExerciseEntity>,
        previous: Previous,
        timer: fr.acano.workout.timer.TimerState?,
    ): ActiveSessionUiState {
        val previousWeights = previous.weights
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
                targetDistanceMeters = planned.targetDistanceMeters,
                lastSessionDistanceMeters = previous.distances[exercise.id],
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
                weightKg = step.plannedWeightKg.takeIf { step.exercise.kind.hasWeight },
                repetitions = repetitions,
            )
            startRestIfNeeded(context, step)
        }
    }

    /** Série mesurée en distance : charge éventuelle + distance parcourue. */
    fun validateDistanceSet(context: Context, distanceMeters: Double) {
        val step = state.value.step ?: return
        viewModelScope.launch {
            repository.recordSet(
                exerciseSessionId = step.exerciseSessionId,
                exerciseId = step.exercise.id,
                setNumber = step.setNumber,
                weightKg = step.plannedWeightKg.takeIf { step.exercise.kind.hasWeight },
                distanceMeters = distanceMeters,
            )
            startRestIfNeeded(context, step)
        }
    }

    /**
     * Enregistre la série chronométrée en attente avec la distance saisie, ou
     * sans distance si [distanceMeters] est nul (« Passer »).
     */
    fun confirmPendingDistance(context: Context, distanceMeters: Double?) {
        val step = state.value.step ?: return
        val pending = pendingDistance.value?.takeIf { it.exerciseSessionId == step.exerciseSessionId } ?: return
        pendingDistance.value = null
        viewModelScope.launch {
            recordTimedSet(step, pending.durationSeconds, distanceMeters)
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
        // La durée réellement chronométrée : ±30 s en cours de série la
        // font diverger de la cible, et c'est l'effort réel qu'on veut suivre.
        val seconds = ActiveTimer.state.value
            ?.takeIf { it.kind == TimerKind.EFFORT }
            ?.let { ((it.totalMs + 500) / 1000).toInt() }
            ?: step.targetDurationSeconds
            ?: return
        if (step.exercise.kind.hasDistance) {
            askDistance(context, step, seconds)
            return
        }
        viewModelScope.launch {
            recordTimedSet(step, seconds, distanceMeters = null)
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
        if (step.exercise.kind.hasDistance) {
            askDistance(context, step, elapsedSeconds)
            return
        }
        viewModelScope.launch {
            recordTimedSet(step, elapsedSeconds, distanceMeters = null)
            // Pas de stop() ici : startRestIfNeeded arrête déjà le chronomètre
            // quand il n'y a pas de récupération à lancer. Enchaîner un stop et
            // un start faisait se croiser destruction et création du service.
            startRestIfNeeded(context, step)
        }
    }

    /** Le chrono s'arrête ; la série attend la distance parcourue avant d'être enregistrée. */
    private fun askDistance(context: Context, step: CurrentStep, durationSeconds: Int) {
        RestTimerService.stop(context)
        pendingDistance.value = PendingDistance(step.exerciseSessionId, step.setNumber, durationSeconds)
    }

    /** Une série chronométrée, avec sa charge si l'exercice en a une (planche lestée). */
    private suspend fun recordTimedSet(step: CurrentStep, durationSeconds: Int, distanceMeters: Double?) {
        repository.recordSet(
            exerciseSessionId = step.exerciseSessionId,
            exerciseId = step.exercise.id,
            setNumber = step.setNumber,
            weightKg = step.plannedWeightKg.takeIf { step.exercise.kind.hasWeight },
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
        )
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
        pendingDistance.value = null
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

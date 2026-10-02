package fr.acano.workout.ui.session

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.domain.SessionProgress
import fr.acano.workout.domain.WorkoutType
import fr.acano.workout.timer.TimerState

/** L'étape affichée : tout ce que l'écran doit montrer sans avoir à recalculer quoi que ce soit. */
data class CurrentStep(
    val exerciseSessionId: Long,
    val exercise: ExerciseEntity,
    val setNumber: Int,
    val plannedSets: Int,
    val plannedWeightKg: Double?,
    val lastSessionWeightKg: Double?,
    /** Séries déjà réalisées aujourd'hui sur cet exercice. */
    val setsDoneToday: List<SetResultEntity>,
    /** Cibles de l'étape, fixées par l'entraînement ; celle qui ne s'applique pas reste nulle. */
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSeconds: Int? = null,
    /** Repos après une série, en secondes. 0 = pas de chrono. */
    val restSeconds: Int = 0,
    val targetDistanceMeters: Int? = null,
    /** Dernière distance des séances précédentes, pour pré-remplir la saisie. */
    val lastSessionDistanceMeters: Double? = null,
) {
    /**
     * La distance proposée pour la série : celle de la série précédente du
     * jour, sinon la cible, sinon celle de la dernière séance.
     */
    val suggestedDistanceMeters: Double?
        get() = setsDoneToday.lastOrNull()?.distanceMeters
            ?: targetDistanceMeters?.toDouble()
            ?: lastSessionDistanceMeters
}

/**
 * Une série chronométrée terminée, dont il reste à noter la distance
 * parcourue (tapis, rameur…) avant de l'enregistrer.
 */
data class PendingDistance(
    val exerciseSessionId: Long,
    val setNumber: Int,
    val durationSeconds: Int,
)

/** Un exercice restant, tel qu'il apparaît dans la feuille « changer d'exercice ». */
data class PendingStep(
    val exerciseSessionId: Long,
    val name: String,
    val plannedSets: Int,
    val completedSets: Int,
    val isCurrent: Boolean,
)

data class ActiveSessionUiState(
    val isLoading: Boolean = true,
    val sessionMissing: Boolean = false,
    val type: WorkoutType? = null,
    val startedAt: Long = 0,
    val progress: SessionProgress? = null,
    val step: CurrentStep? = null,
    val timer: TimerState? = null,
    val isFinished: Boolean = false,
    /** Exercices restants, dans l'ordre : l'étape courante est la première. */
    val pendingSteps: List<PendingStep> = emptyList(),
    /** Non nul quand une série chronométrée attend sa distance. */
    val pendingDistance: PendingDistance? = null,
)

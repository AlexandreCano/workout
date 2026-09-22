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
)

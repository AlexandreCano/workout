package fr.acano.workout.ui.nav

import kotlinx.serialization.Serializable

/** Destinations type-safe : une faute de frappe devient une erreur de compilation. */
sealed interface Route {

    @Serializable data object Home : Route

    @Serializable data object History : Route

    @Serializable data object Exercises : Route

    @Serializable data object Settings : Route

    @Serializable data class ActiveSession(val sessionId: Long) : Route

    @Serializable data class SessionSummary(val sessionId: Long) : Route

    @Serializable data class SessionDetail(val sessionId: Long) : Route

    @Serializable data class ExerciseDetail(val exerciseId: String) : Route

    /** Édition d'un exercice créé par l'utilisateur ; [NEW] pour en créer un. */
    @Serializable data class ExerciseEditor(val exerciseId: String = NEW) : Route {
        companion object {
            const val NEW = ""
        }
    }

    /** Édition d'un entraînement personnalisé ; [NEW] pour en créer un. */
    @Serializable data class WorkoutEditor(val workoutId: Long = NEW) : Route {
        companion object {
            const val NEW = 0L
        }
    }
}

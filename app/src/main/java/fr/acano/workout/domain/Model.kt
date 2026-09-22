package fr.acano.workout.domain

/** Les deux séances du programme. */
enum class WorkoutType(val label: String) {
    UPPER_BODY("Haut du corps"),
    LOWER_BODY("Bas du corps"),
}

/**
 * Détermine l'interface de saisie d'un exercice.
 *
 * - [WEIGHTED_REPS] : poids + répétitions (machines).
 * - [TIMED] : durée fixe, déclenchée par un timer (vélo, planche).
 * - [REPS_ONLY] : quelques répétitions, sans charge (stomach vacuum).
 */
enum class ExerciseKind {
    WEIGHTED_REPS,
    TIMED,
    REPS_ONLY,
}

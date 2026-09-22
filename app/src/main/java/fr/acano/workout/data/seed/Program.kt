package fr.acano.workout.data.seed

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.WorkoutType

/**
 * Le programme, défini en dur : il ne change pas, et le garder en Kotlin évite
 * trois tables « modèle de séance » dont l'application n'a aucun usage.
 * Le catalogue est copié en base au premier lancement pour que les séries
 * enregistrées pointent vers des exercices stables.
 */
object Program {

    const val BIKE = "bike_warmup"
    const val PLANK = "plank"
    const val STOMACH_VACUUM = "stomach_vacuum"

    val exercises: List<ExerciseEntity> = listOf(
        ExerciseEntity(
            id = BIKE,
            name = "Vélo",
            kind = ExerciseKind.TIMED,
            plannedSets = 1,
            targetDurationSeconds = 5 * 60,
            restSeconds = 0,
        ),
        // --- Haut du corps ---
        ExerciseEntity(
            id = "chest_press",
            name = "Chest Press",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 4,
            targetRepsMin = 8,
            targetRepsMax = 12,
            restSeconds = 60,
        ),
        ExerciseEntity(
            id = "pec_deck",
            name = "Pec Deck",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 3,
            targetRepsMin = 10,
            targetRepsMax = 15,
            restSeconds = 60,
        ),
        ExerciseEntity(
            id = "lat_pulldown",
            name = "Tirage vertical",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 3,
            targetRepsMin = 8,
            targetRepsMax = 12,
            restSeconds = 60,
        ),
        ExerciseEntity(
            id = "seated_row",
            name = "Tirage horizontal",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 3,
            targetRepsMin = 8,
            targetRepsMax = 12,
            restSeconds = 60,
        ),
        // --- Bas du corps ---
        ExerciseEntity(
            id = "leg_press",
            name = "Presse à cuisses",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 4,
            targetRepsMin = 8,
            targetRepsMax = 12,
            restSeconds = 60,
            weightStepKg = 5.0,
        ),
        ExerciseEntity(
            id = "leg_curl",
            name = "Leg Curl",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 3,
            targetRepsMin = 10,
            targetRepsMax = 15,
            restSeconds = 60,
        ),
        ExerciseEntity(
            id = "leg_extension",
            name = "Leg Extension",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 3,
            targetRepsMin = 10,
            targetRepsMax = 15,
            restSeconds = 60,
        ),
        ExerciseEntity(
            id = "calf_raise",
            name = "Mollets",
            kind = ExerciseKind.WEIGHTED_REPS,
            plannedSets = 3,
            targetRepsMin = 12,
            targetRepsMax = 20,
            restSeconds = 60,
        ),
        // --- Fin de séance, commun aux deux séances ---
        ExerciseEntity(
            id = PLANK,
            name = "Planche",
            kind = ExerciseKind.TIMED,
            plannedSets = 4,
            targetDurationSeconds = 60,
            restSeconds = 60,
        ),
        ExerciseEntity(
            id = STOMACH_VACUUM,
            name = "Stomach Vacuum",
            kind = ExerciseKind.REPS_ONLY,
            plannedSets = 1,
            targetRepsMin = 3,
            targetRepsMax = 5,
            restSeconds = 0,
        ),
    )

    private val upperBody = listOf(
        BIKE, "chest_press", "pec_deck", "lat_pulldown", "seated_row", PLANK, STOMACH_VACUUM,
    )

    private val lowerBody = listOf(
        BIKE, "leg_press", "leg_curl", "leg_extension", "calf_raise", PLANK, STOMACH_VACUUM,
    )

    fun planFor(type: WorkoutType): List<String> = when (type) {
        WorkoutType.UPPER_BODY -> upperBody
        WorkoutType.LOWER_BODY -> lowerBody
    }

    /** Exercices du catalogue hors échauffement, pour l'onglet « Exercices ». */
    val catalogue: List<ExerciseEntity> get() = exercises.filter { it.id != BIKE }
}

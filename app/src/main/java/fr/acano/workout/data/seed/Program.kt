package fr.acano.workout.data.seed

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.PlannedStep
import fr.acano.workout.domain.WorkoutType

/**
 * Le programme d'origine, défini en dur.
 *
 * Deux choses bien séparées :
 *  - [exercises], les exercices fournis avec l'application : le mouvement seul
 *    (nom, type, pas de charge), copié en base au premier lancement pour que
 *    les séries enregistrées pointent vers des exercices stables ;
 *  - les plans des deux séances, qui portent séries, cibles et repos. Ils ne
 *    servent qu'à créer les entraînements « Haut du corps » et « Bas du corps »
 *    au premier lancement (voir `ProgramSeed`), qui se modifient ensuite comme
 *    n'importe quel entraînement.
 */
object Program {

    const val BIKE = "bike_warmup"
    const val PLANK = "plank"
    const val STOMACH_VACUUM = "stomach_vacuum"

    val exercises: List<ExerciseEntity> = listOf(
        ExerciseEntity(id = BIKE, name = "Vélo", kind = ExerciseKind.TIMED),
        // --- Haut du corps ---
        ExerciseEntity(id = "chest_press", name = "Chest Press", kind = ExerciseKind.WEIGHTED_REPS),
        ExerciseEntity(id = "pec_deck", name = "Pec Deck", kind = ExerciseKind.WEIGHTED_REPS),
        ExerciseEntity(id = "lat_pulldown", name = "Tirage vertical", kind = ExerciseKind.WEIGHTED_REPS),
        ExerciseEntity(id = "seated_row", name = "Tirage horizontal", kind = ExerciseKind.WEIGHTED_REPS),
        // --- Bas du corps ---
        ExerciseEntity(
            id = "leg_press",
            name = "Presse à cuisses",
            kind = ExerciseKind.WEIGHTED_REPS,
            weightStepKg = 5.0,
        ),
        ExerciseEntity(id = "leg_curl", name = "Leg Curl", kind = ExerciseKind.WEIGHTED_REPS),
        ExerciseEntity(id = "leg_extension", name = "Leg Extension", kind = ExerciseKind.WEIGHTED_REPS),
        ExerciseEntity(id = "calf_raise", name = "Mollets", kind = ExerciseKind.WEIGHTED_REPS),
        // --- Fin de séance, commun aux deux séances ---
        ExerciseEntity(id = PLANK, name = "Planche", kind = ExerciseKind.TIMED),
        ExerciseEntity(id = STOMACH_VACUUM, name = "Stomach Vacuum", kind = ExerciseKind.REPS_ONLY),
    )

    private val warmUp = PlannedStep(BIKE, plannedSets = 1, targetDurationSeconds = 5 * 60, restSeconds = 0)

    private val core = listOf(
        PlannedStep(PLANK, plannedSets = 4, targetDurationSeconds = 60, restSeconds = 60),
        PlannedStep(STOMACH_VACUUM, plannedSets = 1, targetRepsMin = 3, targetRepsMax = 5, restSeconds = 0),
    )

    private val upperBody = listOf(warmUp) + listOf(
        PlannedStep("chest_press", plannedSets = 4, targetRepsMin = 8, targetRepsMax = 12),
        PlannedStep("pec_deck", plannedSets = 3, targetRepsMin = 10, targetRepsMax = 15),
        PlannedStep("lat_pulldown", plannedSets = 3, targetRepsMin = 8, targetRepsMax = 12),
        PlannedStep("seated_row", plannedSets = 3, targetRepsMin = 8, targetRepsMax = 12),
    ) + core

    private val lowerBody = listOf(warmUp) + listOf(
        PlannedStep("leg_press", plannedSets = 4, targetRepsMin = 8, targetRepsMax = 12),
        PlannedStep("leg_curl", plannedSets = 3, targetRepsMin = 10, targetRepsMax = 15),
        PlannedStep("leg_extension", plannedSets = 3, targetRepsMin = 10, targetRepsMax = 15),
        PlannedStep("calf_raise", plannedSets = 3, targetRepsMin = 12, targetRepsMax = 20),
    ) + core

    /** Les séances définies par le programme, dans l'ordre d'affichage. */
    val types: List<WorkoutType> = listOf(WorkoutType.UPPER_BODY, WorkoutType.LOWER_BODY)

    fun planFor(type: WorkoutType): List<PlannedStep> = when (type) {
        WorkoutType.UPPER_BODY -> upperBody
        WorkoutType.LOWER_BODY -> lowerBody
        WorkoutType.CUSTOM -> error("Un entraînement personnalisé n'a pas de plan fixe")
    }

    /**
     * Réglages proposés quand on ajoute un exercice à un entraînement : ceux du
     * programme s'il y figure, sinon des valeurs courantes selon son type.
     */
    fun defaultStepFor(exerciseId: String, kind: ExerciseKind): PlannedStep =
        (upperBody + lowerBody).firstOrNull { it.exerciseId == exerciseId }
            ?: when (kind) {
                ExerciseKind.WEIGHTED_REPS ->
                    PlannedStep(exerciseId, plannedSets = 3, targetRepsMin = 8, targetRepsMax = 12)
                ExerciseKind.REPS_ONLY ->
                    PlannedStep(exerciseId, plannedSets = 3, targetRepsMin = 10, targetRepsMax = 15)
                ExerciseKind.TIMED ->
                    PlannedStep(exerciseId, plannedSets = 3, targetDurationSeconds = 60)
            }
}

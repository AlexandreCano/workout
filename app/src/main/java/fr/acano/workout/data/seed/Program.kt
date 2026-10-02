package fr.acano.workout.data.seed

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.PlannedStep
import fr.acano.workout.domain.WorkoutType

/**
 * Le programme d'origine, défini en dur.
 *
 * Deux choses bien séparées :
 *  - [exercises], les exercices fournis avec l'application, décrits par le
 *    catalogue ([ExerciseCatalog]) : le mouvement seul (nom, type, muscles,
 *    matériel), copié en base au premier lancement pour que les séries
 *    enregistrées pointent vers des exercices stables ;
 *  - les plans des deux séances, qui portent séries, cibles et repos. Ils ne
 *    servent qu'à créer les entraînements « Haut du corps » et « Bas du corps »
 *    au premier lancement (voir `ProgramSeed`), qui se modifient ensuite comme
 *    n'importe quel entraînement.
 */
object Program {

    const val BIKE = "bike_warmup"
    const val PLANK = "plank"
    const val STOMACH_VACUUM = "stomach_vacuum"

    val exercises: List<ExerciseEntity> by lazy { ExerciseCatalog.entries.map { it.toEntity() } }

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
        PlannedStep("lying_leg_curl", plannedSets = 3, targetRepsMin = 10, targetRepsMax = 15),
        PlannedStep("leg_extension", plannedSets = 3, targetRepsMin = 10, targetRepsMax = 15),
        PlannedStep("standing_calf_raise", plannedSets = 3, targetRepsMin = 12, targetRepsMax = 20),
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
                // Le cardio se fait d'une traite : une série de dix minutes, sans repos.
                ExerciseKind.TIMED_DISTANCE ->
                    PlannedStep(exerciseId, plannedSets = 1, targetDurationSeconds = 10 * 60, restSeconds = 0)
                ExerciseKind.TIMED ->
                    if (ExerciseCatalog[exerciseId]?.category == ExerciseCategory.CARDIO) {
                        PlannedStep(exerciseId, plannedSets = 1, targetDurationSeconds = 10 * 60, restSeconds = 0)
                    } else {
                        PlannedStep(exerciseId, plannedSets = 3, targetDurationSeconds = 60)
                    }
                ExerciseKind.WEIGHTED_TIMED ->
                    PlannedStep(exerciseId, plannedSets = 3, targetDurationSeconds = 45)
                ExerciseKind.WEIGHTED_DISTANCE ->
                    PlannedStep(exerciseId, plannedSets = 3, targetDistanceMeters = 30, restSeconds = 90)
                ExerciseKind.DISTANCE ->
                    PlannedStep(exerciseId, plannedSets = 1, targetDistanceMeters = 1000, restSeconds = 0)
            }
}

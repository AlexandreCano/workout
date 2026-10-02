package fr.acano.workout.data.db

import androidx.room.Embedded
import androidx.room.Relation
import fr.acano.workout.data.db.entity.CustomWorkoutEntity
import fr.acano.workout.data.db.entity.CustomWorkoutExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity
import fr.acano.workout.domain.StepState

data class ExerciseSessionWithSets(
    @Embedded val exerciseSession: ExerciseSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseSessionId")
    val sets: List<SetResultEntity>,
) {
    /** Ce que la progression de séance a besoin de savoir de cette étape. */
    val stepState: StepState
        get() = StepState(exerciseSession.plannedSets, sets.size, exerciseSession.skipped)
}

data class SessionWithContent(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseSessionEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val exercises: List<ExerciseSessionWithSets>,
) {
    /** Room ne garantit pas l'ordre d'une @Relation : on trie explicitement. */
    val orderedExercises: List<ExerciseSessionWithSets>
        get() = exercises.sortedBy { it.exerciseSession.position }
}

/** Combien de fois un exercice a été fait, et quand pour la dernière fois (séances terminées). */
data class ExerciseUsage(
    val exerciseId: String,
    val sessionCount: Int,
    val lastDoneAt: Long,
)

/** Bilan d'un entraînement : combien de fois il a été terminé, et quand pour la dernière fois. */
data class WorkoutStats(
    val customWorkoutId: Long,
    val sessionCount: Int,
    val lastDoneAt: Long,
)

data class CustomWorkoutWithExercises(
    @Embedded val workout: CustomWorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<CustomWorkoutExerciseEntity>,
) {
    val orderedExercises: List<CustomWorkoutExerciseEntity>
        get() = exercises.sortedBy { it.position }
}

package fr.acano.workout.data.db

import androidx.room.Embedded
import androidx.room.Relation
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity

data class ExerciseSessionWithSets(
    @Embedded val exerciseSession: ExerciseSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseSessionId")
    val sets: List<SetResultEntity>,
)

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

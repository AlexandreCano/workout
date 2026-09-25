package fr.acano.workout.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import fr.acano.workout.data.db.CustomWorkoutWithExercises
import fr.acano.workout.data.db.entity.CustomWorkoutEntity
import fr.acano.workout.data.db.entity.CustomWorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomWorkoutDao {

    @Transaction
    @Query("SELECT * FROM custom_workout ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomWorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM custom_workout WHERE id = :id")
    suspend fun get(id: Long): CustomWorkoutWithExercises?

    @Insert
    suspend fun insertWorkout(workout: CustomWorkoutEntity): Long

    @Query("UPDATE custom_workout SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Insert
    suspend fun insertExercises(items: List<CustomWorkoutExerciseEntity>)

    @Query("DELETE FROM custom_workout_exercise WHERE workoutId = :workoutId")
    suspend fun clearExercises(workoutId: Long)

    @Query(
        """
        SELECT DISTINCT w.name FROM custom_workout w
        INNER JOIN custom_workout_exercise e ON e.workoutId = w.id
        WHERE e.exerciseId = :exerciseId
        ORDER BY w.createdAt
        """,
    )
    suspend fun workoutNamesUsing(exerciseId: String): List<String>

    @Query("DELETE FROM custom_workout_exercise WHERE exerciseId = :exerciseId")
    suspend fun removeExerciseEverywhere(exerciseId: String)

    @Query("UPDATE custom_workout SET name = :name WHERE name IN (:knownNames) AND name != :name")
    suspend fun renameMatching(knownNames: List<String>, name: String)

    @Query("DELETE FROM custom_workout WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Remplace le contenu d'un entraînement. Réécrire la liste entière est plus
     * simple et plus sûr que de calculer un diff, et la transaction garantit
     * qu'on ne se retrouve jamais avec un entraînement vidé à moitié.
     */
    @Transaction
    suspend fun replaceExercises(workoutId: Long, items: List<CustomWorkoutExerciseEntity>) {
        clearExercises(workoutId)
        insertExercises(items)
    }
}

package fr.acano.workout.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercise")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    /** Renomme un exercice de l'application ; ceux de l'utilisateur ne sont jamais touchés. */
    @Query("UPDATE exercise SET name = :name WHERE id = :id AND isCustom = 0 AND name != :name")
    suspend fun renameBuiltIn(id: String, name: String)

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    /**
     * Remet un exercice de l'application en accord avec le catalogue : type,
     * catégorie, muscles et matériel. Le nom (géré par la langue) et le pas de
     * charge ne sont pas touchés.
     */
    @Query(
        """
        UPDATE exercise SET kind = :kind, category = :category, primaryMuscle = :primaryMuscle,
            secondaryMuscles = :secondaryMuscles, equipment = :equipment
        WHERE id = :id AND isCustom = 0
        """,
    )
    suspend fun updateBuiltInDetails(
        id: String,
        kind: ExerciseKind,
        category: ExerciseCategory?,
        primaryMuscle: Muscle?,
        secondaryMuscles: List<Muscle>,
        equipment: Equipment?,
    )

    @Query("UPDATE set_result SET exerciseId = :newId WHERE exerciseId = :oldId")
    suspend fun moveSets(oldId: String, newId: String)

    @Query("UPDATE exercise_session SET exerciseId = :newId WHERE exerciseId = :oldId")
    suspend fun moveSessionSteps(oldId: String, newId: String)

    @Query("UPDATE custom_workout_exercise SET exerciseId = :newId WHERE exerciseId = :oldId")
    suspend fun moveWorkoutSteps(oldId: String, newId: String)

    @Query("DELETE FROM exercise WHERE id = :id AND isCustom = 0")
    suspend fun deleteBuiltIn(id: String)

    /**
     * Fait passer tout ce qui référence l'exercice [oldId] — séries, étapes de
     * séance et d'entraînement — sous [newId], puis supprime l'ancien. Tout ou
     * rien : une interruption ne laisse pas l'historique à cheval sur les deux.
     */
    @Transaction
    suspend fun replaceBuiltInId(oldId: String, newId: String) {
        moveSets(oldId, newId)
        moveSessionSteps(oldId, newId)
        moveWorkoutSteps(oldId, newId)
        deleteBuiltIn(oldId)
    }
}

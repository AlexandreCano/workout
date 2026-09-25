package fr.acano.workout.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import fr.acano.workout.data.db.entity.ExerciseEntity
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
}

package fr.acano.workout.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- Création ---

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertExerciseSessions(items: List<ExerciseSessionEntity>)

    @Insert
    suspend fun insertSetResult(set: SetResultEntity): Long

    // --- Séance en cours ---

    @Transaction
    @Query("SELECT * FROM workout_session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionWithContent?>

    @Query("SELECT * FROM workout_session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionWithContent?>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): SessionWithContent?

    @Query("UPDATE exercise_session SET plannedWeightKg = :weightKg WHERE id = :exerciseSessionId")
    suspend fun updatePlannedWeight(exerciseSessionId: Long, weightKg: Double?)

    @Query("UPDATE exercise_session SET position = :position WHERE id = :exerciseSessionId")
    suspend fun updatePosition(exerciseSessionId: Long, position: Int)

    /**
     * Renumérote toutes les étapes d'une séance d'un seul coup.
     * La transaction évite qu'une interruption laisse un ordre partiellement réécrit.
     */
    @Transaction
    suspend fun applyOrder(orderedExerciseSessionIds: List<Long>) {
        orderedExerciseSessionIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }

    @Query("UPDATE workout_session SET endedAt = :endedAt, starAwarded = 1 WHERE id = :sessionId AND endedAt IS NULL")
    suspend fun finishSession(sessionId: Long, endedAt: Long)

    @Query("DELETE FROM workout_session WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query(
        """
        DELETE FROM set_result
        WHERE id = (
            SELECT id FROM set_result
            WHERE exerciseSessionId = :exerciseSessionId
            ORDER BY setNumber DESC LIMIT 1
        )
        """,
    )
    suspend fun deleteLastSet(exerciseSessionId: Long)

    // --- Historique ---

    @Transaction
    @Query("SELECT * FROM workout_session WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedSessions(): Flow<List<SessionWithContent>>

    @Query("SELECT * FROM workout_session WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeLastFinishedSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT COUNT(*) FROM workout_session WHERE starAwarded = 1")
    fun observeStarCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_session WHERE starAwarded = 1 AND type = :type")
    fun observeStarCountByType(type: WorkoutType): Flow<Int>

    /**
     * Dernier poids réellement utilisé sur un exercice, toutes séances terminées confondues.
     * Sert à pré-remplir la charge de la séance suivante.
     */
    @Query(
        """
        SELECT weightKg FROM set_result
        WHERE exerciseId = :exerciseId AND weightKg IS NOT NULL
        ORDER BY completedAt DESC LIMIT 1
        """,
    )
    suspend fun lastWeightFor(exerciseId: String): Double?

    @Query(
        """
        SELECT weightKg FROM set_result
        WHERE exerciseId = :exerciseId AND weightKg IS NOT NULL
        ORDER BY completedAt DESC LIMIT 1
        """,
    )
    fun observeLastWeightFor(exerciseId: String): Flow<Double?>

    @Query("SELECT * FROM set_result WHERE exerciseId = :exerciseId ORDER BY completedAt ASC")
    fun observeSetsForExercise(exerciseId: String): Flow<List<SetResultEntity>>

    /** Séries chargées les plus récentes, tous exercices confondus : alimente le bloc « progression récente ». */
    @Query("SELECT * FROM set_result WHERE weightKg IS NOT NULL ORDER BY completedAt DESC LIMIT 500")
    fun observeRecentWeightedSets(): Flow<List<SetResultEntity>>

    /**
     * Séries chargées des séances *autres* que celle en cours : permet d'afficher
     * « dernière séance : 42,5 kg » sans que les séries du jour ne polluent la référence.
     */
    @Query(
        """
        SELECT sr.* FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        WHERE es.sessionId != :sessionId AND sr.weightKg IS NOT NULL
        ORDER BY sr.completedAt DESC
        """,
    )
    fun observePreviousWeights(sessionId: Long): Flow<List<SetResultEntity>>
}

package fr.acano.workout.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import fr.acano.workout.data.db.ExerciseUsage
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.WorkoutStats
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity
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

    @Query(
        """
        SELECT customWorkoutId, COUNT(*) AS sessionCount, MAX(startedAt) AS lastDoneAt
        FROM workout_session
        WHERE starAwarded = 1 AND customWorkoutId IS NOT NULL
        GROUP BY customWorkoutId
        """,
    )
    fun observeWorkoutStats(): Flow<List<WorkoutStats>>

    // --- Séries des séances terminées ---
    //
    // Toutes les statistiques ne lisent que des séances terminées : une séance
    // en cours (ou mise en pause) n'a encore rien prouvé, et une séance
    // abandonnée est supprimée avec ses séries. La jointure est la même partout.

    /**
     * Dernier poids réellement utilisé sur un exercice, séances terminées seulement.
     * Sert à pré-remplir la charge de la séance suivante.
     */
    @Query(
        """
        SELECT sr.weightKg FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        INNER JOIN workout_session ws ON ws.id = es.sessionId
        WHERE sr.exerciseId = :exerciseId AND sr.weightKg IS NOT NULL AND ws.endedAt IS NOT NULL
        ORDER BY sr.completedAt DESC LIMIT 1
        """,
    )
    suspend fun lastWeightFor(exerciseId: String): Double?

    @Query(
        """
        SELECT sr.weightKg FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        INNER JOIN workout_session ws ON ws.id = es.sessionId
        WHERE sr.exerciseId = :exerciseId AND sr.weightKg IS NOT NULL AND ws.endedAt IS NOT NULL
        ORDER BY sr.completedAt DESC LIMIT 1
        """,
    )
    fun observeLastWeightFor(exerciseId: String): Flow<Double?>

    /** Historique d'un exercice (détail, courbe de charge). */
    @Query(
        """
        SELECT sr.* FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        INNER JOIN workout_session ws ON ws.id = es.sessionId
        WHERE sr.exerciseId = :exerciseId AND ws.endedAt IS NOT NULL
        ORDER BY sr.completedAt ASC
        """,
    )
    fun observeSetsForExercise(exerciseId: String): Flow<List<SetResultEntity>>

    /** Séries chargées les plus récentes, tous exercices confondus : « progression récente », dernières charges. */
    @Query(
        """
        SELECT sr.* FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        INNER JOIN workout_session ws ON ws.id = es.sessionId
        WHERE sr.weightKg IS NOT NULL AND ws.endedAt IS NOT NULL
        ORDER BY sr.completedAt DESC LIMIT 500
        """,
    )
    fun observeRecentWeightedSets(): Flow<List<SetResultEntity>>

    /**
     * Séries chargées des séances terminées autres que [sessionId] : permet d'afficher
     * « dernière séance : 42,5 kg » sans que les séries du jour ne polluent la référence.
     */
    @Query(
        """
        SELECT sr.* FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        INNER JOIN workout_session ws ON ws.id = es.sessionId
        WHERE es.sessionId != :sessionId AND sr.weightKg IS NOT NULL AND ws.endedAt IS NOT NULL
        ORDER BY sr.completedAt DESC
        """,
    )
    fun observePreviousWeights(sessionId: Long): Flow<List<SetResultEntity>>

    @Query(
        """
        SELECT sr.exerciseId AS exerciseId, COUNT(DISTINCT es.sessionId) AS sessionCount,
            MAX(ws.startedAt) AS lastDoneAt
        FROM set_result sr
        INNER JOIN exercise_session es ON es.id = sr.exerciseSessionId
        INNER JOIN workout_session ws ON ws.id = es.sessionId
        WHERE ws.endedAt IS NOT NULL
        GROUP BY sr.exerciseId
        """,
    )
    fun observeExerciseUsage(): Flow<List<ExerciseUsage>>

    // --- Abandon ---

    @Query("DELETE FROM set_result WHERE exerciseSessionId IN (SELECT id FROM exercise_session WHERE sessionId = :sessionId)")
    suspend fun deleteSetsOfSession(sessionId: Long)

    @Query("DELETE FROM exercise_session WHERE sessionId = :sessionId")
    suspend fun deleteStepsOfSession(sessionId: Long)

    /**
     * Supprime une séance et tout ce qui en dépend, explicitement : on ne s'en
     * remet pas aux ON DELETE CASCADE, qui ne jouent que si les clés étrangères
     * sont actives sur la connexion utilisée.
     */
    @Transaction
    suspend fun deleteSessionEntirely(sessionId: Long) {
        deleteSetsOfSession(sessionId)
        deleteStepsOfSession(sessionId)
        deleteSession(sessionId)
    }
}

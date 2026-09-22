package fr.acano.workout.data.repository

import fr.acano.workout.data.db.ExerciseSessionWithSets
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.dao.ExerciseDao
import fr.acano.workout.data.db.dao.WorkoutDao
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Insère le programme au premier lancement, et complète le catalogue après une mise à jour. */
    suspend fun ensureSeeded() {
        val existing = exerciseDao.getAll().associateBy { it.id }
        val missing = Program.exercises.filter { it.id !in existing }
        if (missing.isNotEmpty()) exerciseDao.insertAll(missing)
    }

    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    fun observeExercise(id: String): Flow<ExerciseEntity?> = exerciseDao.observeById(id)

    fun observeSetsForExercise(id: String): Flow<List<SetResultEntity>> =
        workoutDao.observeSetsForExercise(id)

    fun observeLastWeight(exerciseId: String): Flow<Double?> =
        workoutDao.observeLastWeightFor(exerciseId)

    fun observeRecentWeightedSets(): Flow<List<SetResultEntity>> =
        workoutDao.observeRecentWeightedSets()

    /** Dernier poids connu par exercice, en excluant la séance en cours. */
    fun observePreviousWeights(sessionId: Long): Flow<Map<String, Double>> =
        workoutDao.observePreviousWeights(sessionId).map { sets ->
            sets.groupBy { it.exerciseId }
                .mapNotNull { (id, rows) -> rows.firstOrNull()?.weightKg?.let { id to it } }
                .toMap()
        }

    // --- Séance ---

    fun observeActiveSession(): Flow<SessionWithContent?> = workoutDao.observeActiveSession()

    fun observeSession(sessionId: Long): Flow<SessionWithContent?> =
        workoutDao.observeSession(sessionId)

    fun observeFinishedSessions(): Flow<List<SessionWithContent>> =
        workoutDao.observeFinishedSessions()

    fun observeLastFinishedSession(): Flow<WorkoutSessionEntity?> =
        workoutDao.observeLastFinishedSession()

    fun observeStarCount(): Flow<Int> = workoutDao.observeStarCount()

    fun observeStarCount(type: WorkoutType): Flow<Int> = workoutDao.observeStarCountByType(type)

    /**
     * Démarre une séance : crée la séance et ses étapes, en pré-remplissant chaque charge
     * avec le dernier poids réellement utilisé sur cet exercice.
     * Si une séance est déjà en cours, on la renvoie telle quelle plutôt que d'en ouvrir une seconde.
     */
    suspend fun startSession(type: WorkoutType): Long {
        workoutDao.getActiveSession()?.let { return it.id }

        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(type = type, startedAt = now()),
        )
        val plan = Program.planFor(type)
        val catalogue = exerciseDao.getAll().associateBy { it.id }
        val steps = plan.mapIndexed { index, exerciseId ->
            val exercise = catalogue.getValue(exerciseId)
            ExerciseSessionEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                position = index,
                plannedSets = exercise.plannedSets,
                plannedWeightKg = workoutDao.lastWeightFor(exerciseId),
            )
        }
        workoutDao.insertExerciseSessions(steps)
        return sessionId
    }

    suspend fun updatePlannedWeight(exerciseSessionId: Long, weightKg: Double?) {
        workoutDao.updatePlannedWeight(exerciseSessionId, weightKg)
    }

    // --- Ordre des exercices ---
    //
    // Réordonner ne touche qu'aux étapes restantes : celles qui sont entièrement
    // terminées gardent leur place en tête. L'étape courante étant toujours la
    // première incomplète, cet invariant suffit à garantir que la progression
    // (« 3 / 7 ») et les séries déjà enregistrées restent cohérentes.
    //
    // L'ordre modifié vaut pour cette séance seulement : le programme de référence
    // reste celui de Program.planFor().

    /** Place cet exercice en tête des exercices restants : « je le fais maintenant ». */
    suspend fun doStepNow(sessionId: Long, exerciseSessionId: Long) {
        reorderPendingSteps(sessionId) { pending ->
            val target = pending.firstOrNull { it.exerciseSession.id == exerciseSessionId }
                ?: return@reorderPendingSteps pending
            listOf(target) + pending.filterNot { it.exerciseSession.id == exerciseSessionId }
        }
    }

    /** Décale cet exercice d'un cran : « la machine est prise, je reviendrai ». */
    suspend fun postponeStep(sessionId: Long, exerciseSessionId: Long) {
        reorderPendingSteps(sessionId) { pending ->
            val index = pending.indexOfFirst { it.exerciseSession.id == exerciseSessionId }
            if (index < 0 || index == pending.lastIndex) {
                return@reorderPendingSteps pending
            }
            pending.toMutableList().apply { add(index + 1, removeAt(index)) }
        }
    }

    private suspend fun reorderPendingSteps(
        sessionId: Long,
        transform: (List<ExerciseSessionWithSets>) -> List<ExerciseSessionWithSets>,
    ) {
        val steps = workoutDao.getSession(sessionId)?.orderedExercises ?: return
        val (completed, pending) = steps.partition { it.sets.size >= it.exerciseSession.plannedSets }
        workoutDao.applyOrder((completed + transform(pending)).map { it.exerciseSession.id })
    }

    suspend fun recordSet(
        exerciseSessionId: Long,
        exerciseId: String,
        setNumber: Int,
        weightKg: Double? = null,
        repetitions: Int? = null,
        durationSeconds: Int? = null,
    ) {
        workoutDao.insertSetResult(
            SetResultEntity(
                exerciseSessionId = exerciseSessionId,
                exerciseId = exerciseId,
                setNumber = setNumber,
                weightKg = weightKg,
                repetitions = repetitions,
                durationSeconds = durationSeconds,
                completedAt = now(),
            ),
        )
    }

    suspend fun undoLastSet(exerciseSessionId: Long) {
        workoutDao.deleteLastSet(exerciseSessionId)
    }

    /** Clôture la séance et attribue l'étoile. La requête est idempotente : une séance ne rapporte qu'une étoile. */
    suspend fun finishSession(sessionId: Long) {
        workoutDao.finishSession(sessionId, now())
    }

    /** Abandonne une séance en cours : elle disparaît de l'historique, ses séries avec (CASCADE). */
    suspend fun abortSession(sessionId: Long) {
        workoutDao.deleteSession(sessionId)
    }

    suspend fun session(sessionId: Long): SessionWithContent? = workoutDao.getSession(sessionId)
}

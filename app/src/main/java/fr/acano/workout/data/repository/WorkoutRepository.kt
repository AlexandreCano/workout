package fr.acano.workout.data.repository

import fr.acano.workout.data.db.CustomWorkoutWithExercises
import fr.acano.workout.data.db.ExerciseSessionWithSets
import fr.acano.workout.data.db.ExerciseUsage
import fr.acano.workout.data.db.SessionWithContent
import fr.acano.workout.data.db.WorkoutStats
import fr.acano.workout.data.db.dao.CustomWorkoutDao
import fr.acano.workout.data.db.dao.ExerciseDao
import fr.acano.workout.data.db.dao.WorkoutDao
import fr.acano.workout.data.db.entity.CustomWorkoutEntity
import fr.acano.workout.data.db.entity.CustomWorkoutExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.ExerciseSessionEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.db.entity.WorkoutSessionEntity
import fr.acano.workout.data.seed.ExerciseCatalog
import fr.acano.workout.data.seed.LocalizedNames
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.PlannedStep
import fr.acano.workout.domain.SetEffort
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val customWorkoutDao: CustomWorkoutDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /**
     * Insère le catalogue au premier lancement, le complète après une mise à
     * jour, et remet les exercices déjà présents en accord avec lui (muscles,
     * matériel…) : une installation ancienne reçoit ainsi les mêmes détails
     * qu'une nouvelle. Les exercices renommés (voir [ExerciseCatalog.renamedIds])
     * emportent leur historique sous leur nouvel identifiant.
     */
    suspend fun ensureSeeded() {
        val existing = exerciseDao.getAll().associateBy { it.id }
        val missing = Program.exercises.filter { it.id !in existing }
        if (missing.isNotEmpty()) exerciseDao.insertAll(missing)
        ExerciseCatalog.renamedIds.forEach { (oldId, newId) ->
            if (existing[oldId]?.isCustom == false) exerciseDao.replaceBuiltInId(oldId, newId)
        }
        Program.exercises.forEach { expected ->
            val current = existing[expected.id]?.takeUnless { it.isCustom } ?: return@forEach
            val upToDate = current.kind == expected.kind &&
                current.category == expected.category &&
                current.primaryMuscle == expected.primaryMuscle &&
                current.secondaryMuscles == expected.secondaryMuscles &&
                current.equipment == expected.equipment
            if (!upToDate) {
                exerciseDao.updateBuiltInDetails(
                    id = expected.id,
                    kind = expected.kind,
                    category = expected.category,
                    primaryMuscle = expected.primaryMuscle,
                    secondaryMuscles = expected.secondaryMuscles,
                    equipment = expected.equipment,
                )
            }
        }
    }

    /** Tous les exercices, archivés compris : l'historique a besoin de leurs noms. */
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    /** Met le contenu fourni par l'application dans la langue du téléphone (voir [LocalizedNames]). */
    suspend fun applyLocalizedNames(names: LocalizedNames) {
        names.exercises.forEach { (id, name) -> exerciseDao.renameBuiltIn(id, name) }
        names.programs.forEach { (type, name) ->
            val known = names.knownProgramNames[type].orEmpty()
            customWorkoutDao.renameMatching(known, name)
            workoutDao.renameSessionsMatching(known, name)
        }
    }

    /** Le catalogue proposé à l'utilisateur : les exercices archivés en sont retirés. */
    fun observeCatalogue(): Flow<List<ExerciseEntity>> =
        exerciseDao.observeAll().map { all -> all.filterNot { it.isArchived } }

    suspend fun exercise(id: String): ExerciseEntity? = exerciseDao.getById(id)

    /**
     * Crée ou met à jour un exercice de l'utilisateur. Les exercices du programme
     * ne passent jamais par ici : ils sont décrits par le code et re-semés.
     */
    suspend fun saveCustomExercise(exercise: ExerciseEntity) {
        require(exercise.isCustom) { "Seuls les exercices créés par l'utilisateur sont modifiables" }
        require(exercise.name.isNotBlank()) { "Un exercice doit avoir un nom" }
        exerciseDao.upsert(exercise.copy(name = exercise.name.trim()))
    }

    /** Noms des entraînements qui contiennent l'exercice, pour prévenir avant de le supprimer. */
    suspend fun workoutsUsing(exerciseId: String): List<String> =
        customWorkoutDao.workoutNamesUsing(exerciseId)

    /**
     * Supprime un exercice de l'utilisateur.
     *
     * Il est retiré des entraînements et du catalogue, mais seulement archivé :
     * les séries déjà enregistrées y font référence, et l'historique doit
     * continuer d'afficher son nom. Les exercices de l'application ne se
     * suppriment pas.
     */
    suspend fun deleteCustomExercise(id: String) {
        val exercise = requireNotNull(exerciseDao.getById(id)) { "Exercice $id introuvable" }
        require(exercise.isCustom) { "Les exercices de l'application ne se suppriment pas" }
        customWorkoutDao.removeExerciseEverywhere(id)
        exerciseDao.upsert(exercise.copy(archivedAt = now(), imagePath = null))
    }

    fun observeExercise(id: String): Flow<ExerciseEntity?> = exerciseDao.observeById(id)

    fun observeSetsForExercise(id: String): Flow<List<SetResultEntity>> =
        workoutDao.observeSetsForExercise(id)

    fun observeLastWeight(exerciseId: String): Flow<Double?> =
        workoutDao.observeLastWeightFor(exerciseId)

    /** Nombre de séances et date de la dernière, par exercice. */
    fun observeExerciseUsage(): Flow<Map<String, ExerciseUsage>> =
        workoutDao.observeExerciseUsage().map { rows -> rows.associateBy { it.exerciseId } }

    fun observeRecentWeightedSets(): Flow<List<SetResultEntity>> =
        workoutDao.observeRecentWeightedSets()

    /** Dernier poids connu par exercice, en excluant la séance en cours. */
    fun observePreviousWeights(sessionId: Long): Flow<Map<String, Double>> =
        workoutDao.observePreviousWeights(sessionId).map { sets ->
            sets.groupBy { it.exerciseId }
                .mapNotNull { (id, rows) -> rows.firstOrNull()?.weightKg?.let { id to it } }
                .toMap()
        }

    /**
     * Ressenti de la dernière série chargée par exercice, en excluant la séance
     * en cours : il accompagne la dernière charge connue (voir [observePreviousWeights]).
     */
    fun observePreviousEfforts(sessionId: Long): Flow<Map<String, SetEffort>> =
        workoutDao.observePreviousWeights(sessionId).map { sets ->
            sets.groupBy { it.exerciseId }
                .mapNotNull { (id, rows) -> rows.firstOrNull()?.effort?.let { id to it } }
                .toMap()
        }

    /** Dernière distance connue par exercice, en excluant la séance en cours. */
    fun observePreviousDistances(sessionId: Long): Flow<Map<String, Double>> =
        workoutDao.observePreviousDistances(sessionId).map { sets ->
            sets.groupBy { it.exerciseId }
                .mapNotNull { (id, rows) -> rows.firstOrNull()?.distanceMeters?.let { id to it } }
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

    /** Bilan de chaque entraînement, indexé par son identifiant. */
    fun observeWorkoutStats(): Flow<Map<Long, WorkoutStats>> =
        workoutDao.observeWorkoutStats().map { rows -> rows.associateBy { it.customWorkoutId } }

    /**
     * Démarre une séance à partir d'un entraînement : crée la séance et ses étapes, en
     * pré-remplissant chaque charge avec le dernier poids réellement utilisé sur l'exercice.
     * Si une séance est déjà en cours, on la renvoie telle quelle plutôt que d'en ouvrir une seconde.
     */
    suspend fun startSession(workoutId: Long): Long {
        workoutDao.getActiveSession()?.let { return it.id }

        val workout = requireNotNull(customWorkoutDao.get(workoutId)) {
            "Entraînement $workoutId introuvable"
        }
        require(workout.exercises.isNotEmpty()) { "L'entraînement $workoutId ne contient aucun exercice" }
        val plan = workout.orderedExercises.map {
            PlannedStep(
                exerciseId = it.exerciseId,
                plannedSets = it.plannedSets,
                targetRepsMin = it.targetRepsMin,
                targetRepsMax = it.targetRepsMax,
                targetDurationSeconds = it.targetDurationSeconds,
                restSeconds = it.restSeconds,
                targetDistanceMeters = it.targetDistanceMeters,
            )
        }
        return createSession(
            WorkoutSessionEntity(
                type = WorkoutType.CUSTOM,
                startedAt = now(),
                customWorkoutId = workout.workout.id,
                name = workout.workout.name,
            ),
            plan,
        )
    }

    private suspend fun createSession(
        session: WorkoutSessionEntity,
        plan: List<PlannedStep>,
    ): Long {
        val sessionId = workoutDao.insertSession(session)
        val steps = plan.mapIndexed { index, step ->
            ExerciseSessionEntity(
                sessionId = sessionId,
                exerciseId = step.exerciseId,
                position = index,
                plannedSets = step.plannedSets,
                plannedWeightKg = workoutDao.lastWeightFor(step.exerciseId),
                targetRepsMin = step.targetRepsMin,
                targetRepsMax = step.targetRepsMax,
                targetDurationSeconds = step.targetDurationSeconds,
                restSeconds = step.restSeconds,
                targetDistanceMeters = step.targetDistanceMeters,
            )
        }
        workoutDao.insertExerciseSessions(steps)
        return sessionId
    }

    // --- Entraînements ---

    fun observeCustomWorkouts(): Flow<List<CustomWorkoutWithExercises>> =
        customWorkoutDao.observeAll()

    suspend fun customWorkout(id: Long): CustomWorkoutWithExercises? = customWorkoutDao.get(id)

    /**
     * Crée ([workoutId] nul) ou remplace un entraînement personnalisé.
     * [steps] liste, dans l'ordre, chaque exercice, son nombre de séries et ses cibles.
     */
    suspend fun saveCustomWorkout(
        workoutId: Long?,
        name: String,
        steps: List<PlannedStep>,
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Un entraînement doit avoir un nom" }
        require(steps.isNotEmpty()) { "Un entraînement doit contenir au moins un exercice" }

        val id = if (workoutId == null) {
            customWorkoutDao.insertWorkout(CustomWorkoutEntity(name = trimmed, createdAt = now()))
        } else {
            customWorkoutDao.rename(workoutId, trimmed)
            workoutId
        }
        customWorkoutDao.replaceExercises(
            id,
            steps.mapIndexed { index, step ->
                CustomWorkoutExerciseEntity(
                    workoutId = id,
                    exerciseId = step.exerciseId,
                    position = index,
                    plannedSets = step.plannedSets.coerceAtLeast(1),
                    targetRepsMin = step.targetRepsMin,
                    targetRepsMax = step.targetRepsMax,
                    targetDurationSeconds = step.targetDurationSeconds,
                    restSeconds = step.restSeconds.coerceAtLeast(0),
                    targetDistanceMeters = step.targetDistanceMeters,
                )
            },
        )
        return id
    }

    /** Supprime le modèle. Les séances déjà réalisées avec restent dans l'historique. */
    suspend fun deleteCustomWorkout(id: Long) {
        customWorkoutDao.delete(id)
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
    // L'ordre modifié vaut pour cette séance seulement : l'entraînement d'origine
    // garde le sien.

    /**
     * Applique un ordre arbitraire aux exercices restants.
     *
     * [orderedExerciseSessionIds] doit contenir exactement les étapes non terminées.
     * Toute étape absente de la liste est conservée, à la suite et dans son ordre
     * d'origine : l'appel reste sûr si l'écran s'appuie sur un état légèrement
     * périmé, par exemple si une série vient d'être validée ailleurs.
     */
    suspend fun applyPendingOrder(sessionId: Long, orderedExerciseSessionIds: List<Long>) {
        reorderPendingSteps(sessionId) { pending ->
            val byId = pending.associateBy { it.exerciseSession.id }
            val moved = orderedExerciseSessionIds.mapNotNull(byId::get)
            val movedIds = moved.map { it.exerciseSession.id }.toSet()
            moved + pending.filterNot { it.exerciseSession.id in movedIds }
        }
    }

    private suspend fun reorderPendingSteps(
        sessionId: Long,
        transform: (List<ExerciseSessionWithSets>) -> List<ExerciseSessionWithSets>,
    ) {
        val steps = workoutDao.getSession(sessionId)?.orderedExercises ?: return
        val (completed, pending) = steps.partition { it.stepState.isComplete }
        workoutDao.applyOrder((completed + transform(pending)).map { it.exerciseSession.id })
    }

    suspend fun recordSet(
        exerciseSessionId: Long,
        exerciseId: String,
        setNumber: Int,
        weightKg: Double? = null,
        repetitions: Int? = null,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
        effort: SetEffort? = null,
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
                distanceMeters = distanceMeters,
                effort = effort,
            ),
        )
    }

    /** Une série de plus pour cette étape, dans cette séance seulement : l'entraînement n'est pas modifié. */
    suspend fun addPlannedSet(exerciseSessionId: Long) {
        workoutDao.addPlannedSet(exerciseSessionId)
    }

    /** Passe l'étape : ses séries restantes sont abandonnées, la séance continue avec la suivante. */
    suspend fun skipStep(exerciseSessionId: Long) {
        workoutDao.markSkipped(exerciseSessionId)
    }

    suspend fun undoLastSet(exerciseSessionId: Long) {
        workoutDao.deleteLastSet(exerciseSessionId)
    }

    /** Clôture la séance et attribue l'étoile. La requête est idempotente : une séance ne rapporte qu'une étoile. */
    suspend fun finishSession(sessionId: Long) {
        workoutDao.finishSession(sessionId, now())
    }

    /** Abandonne une séance en cours : elle disparaît entièrement, séries comprises, sans laisser de trace dans les statistiques. */
    suspend fun abortSession(sessionId: Long) {
        workoutDao.deleteSessionEntirely(sessionId)
    }

    suspend fun session(sessionId: Long): SessionWithContent? = workoutDao.getSession(sessionId)
}

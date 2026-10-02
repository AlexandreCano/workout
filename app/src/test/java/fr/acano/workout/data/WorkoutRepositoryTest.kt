package fr.acano.workout.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.LocalizedNames
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import fr.acano.workout.domain.PlannedStep
import fr.acano.workout.domain.SetEffort
import fr.acano.workout.domain.StepState
import fr.acano.workout.domain.WorkoutProgression
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests du repository sur une vraie base Room en mémoire : c'est là que vivent les invariants. */
@RunWith(RobolectricTestRunner::class)
// Robolectric 4.16 ne fournit pas encore d'image d'API 37 : les tests tournent sur 36,
// ce qui est sans effet sur Room et la logique testée ici.
@Config(sdk = [36])
class WorkoutRepositoryTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: WorkoutRepository
    private var clock = 1_000_000L

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).addCallback(WorkoutDatabase.callback).allowMainThreadQueries().build()
        repository = WorkoutRepository(
            database.exerciseDao(),
            database.workoutDao(),
            database.customWorkoutDao(),
        ) { clock }
        repository.ensureSeeded()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `le programme est seme au premier lancement`() = runTest {
        assertEquals(Program.exercises.size, database.exerciseDao().count())
    }

    @Test
    fun `semer deux fois ne duplique rien`() = runTest {
        repository.ensureSeeded()
        assertEquals(Program.exercises.size, database.exerciseDao().count())
    }

    @Test
    fun `demarrer une seance cree les sept etapes dans l ordre`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val session = repository.session(sessionId)!!

        assertEquals(7, session.orderedExercises.size)
        assertEquals(
            Program.planFor(WorkoutType.UPPER_BODY).map { it.exerciseId },
            session.orderedExercises.map { it.exerciseSession.exerciseId },
        )
    }

    @Test
    fun `une seule seance peut etre en cours a la fois`() = runTest {
        val first = startProgram(WorkoutType.UPPER_BODY)
        val second = startProgram(WorkoutType.LOWER_BODY)

        assertEquals(first, second)
    }

    @Test
    fun `la charge de la seance precedente est pre remplie`() = runTest {
        val first = startProgram(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(first, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 42.5, repetitions = 10)
        finishEntirely(first)

        clock += 86_400_000L
        val second = startProgram(WorkoutType.UPPER_BODY)
        val session = repository.session(second)!!
        val planned = session.orderedExercises
            .first { it.exerciseSession.exerciseId == "chest_press" }
            .exerciseSession
            .plannedWeightKg

        assertEquals(42.5, planned!!, 0.001)
    }

    @Test
    fun `la premiere seance n a aucune charge pre remplie`() = runTest {
        val sessionId = startProgram(WorkoutType.LOWER_BODY)
        val session = repository.session(sessionId)!!

        assertTrue(session.orderedExercises.all { it.exerciseSession.plannedWeightKg == null })
    }

    @Test
    fun `une seance interrompue est reprise sur la serie exacte`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val bike = stepFor(sessionId, Program.BIKE)
        repository.recordSet(bike, Program.BIKE, setNumber = 1, durationSeconds = 300)
        val chestPress = stepFor(sessionId, "chest_press")
        repeat(2) { index ->
            repository.recordSet(chestPress, "chest_press", setNumber = index + 1, weightKg = 45.0, repetitions = 11)
        }

        // Simule une réouverture de l'application : on relit tout depuis la base.
        val reopened = repository.observeActiveSession().first()!!
        val progress = WorkoutProgression.compute(
            reopened.orderedExercises.map { StepState(it.exerciseSession.plannedSets, it.sets.size) },
        )

        assertEquals(1, progress.currentStepIndex)
        assertEquals(3, progress.currentSetNumber)
    }

    @Test
    fun `annuler la derniere serie revient en arriere d une serie`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(sessionId, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 40.0, repetitions = 12)
        repository.recordSet(chestPress, "chest_press", setNumber = 2, weightKg = 40.0, repetitions = 11)

        repository.undoLastSet(chestPress)

        val sets = repository.session(sessionId)!!
            .orderedExercises
            .first { it.exerciseSession.id == chestPress }
            .sets
        assertEquals(1, sets.size)
        assertEquals(1, sets.single().setNumber)
    }

    @Test
    fun `ajouter une serie allonge l etape de la seance sans toucher a l entrainement`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(sessionId, "chest_press")
        val planned = plannedSetsOf(sessionId, chestPress)

        repository.addPlannedSet(chestPress)

        assertEquals(planned + 1, plannedSetsOf(sessionId, chestPress))
        val workoutStep = repository.observeCustomWorkouts().first()
            .first { it.workout.name == WorkoutType.UPPER_BODY.label }
            .exercises.first { it.exerciseId == "chest_press" }
        assertEquals(planned, workoutStep.plannedSets)
    }

    @Test
    fun `passer une etape enchaine sur la suivante et garde les series faites`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(sessionId, Program.BIKE), Program.BIKE, setNumber = 1, durationSeconds = 300)
        val chestPress = stepFor(sessionId, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 40.0, repetitions = 10)

        repository.skipStep(chestPress)

        val session = repository.session(sessionId)!!
        val progress = WorkoutProgression.compute(session.orderedExercises.map { it.stepState })
        assertEquals(2, progress.currentStepIndex)
        assertEquals(1, session.orderedExercises.first { it.exerciseSession.id == chestPress }.sets.size)
    }

    @Test
    fun `le ressenti de la derniere serie de la seance precedente est retrouve`() = runTest {
        val first = startProgram(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(first, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 40.0, repetitions = 12, effort = SetEffort.HARD)
        clock += 1_000L
        repository.recordSet(chestPress, "chest_press", setNumber = 2, weightKg = 40.0, repetitions = 12, effort = SetEffort.EASY)
        finishEntirely(first)

        clock += 86_400_000L
        val second = startProgram(WorkoutType.UPPER_BODY)

        assertEquals(SetEffort.EASY, repository.observePreviousEfforts(second).first()["chest_press"])
    }

    @Test
    fun `terminer une seance attribue exactement une etoile`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        repository.finishSession(sessionId)
        repository.finishSession(sessionId)

        assertEquals(1, repository.observeStarCount().first())
    }

    @Test
    fun `une seance en cours ne rapporte pas d etoile`() = runTest {
        startProgram(WorkoutType.UPPER_BODY)

        assertEquals(0, repository.observeStarCount().first())
    }

    @Test
    fun `les etoiles sont comptees par entrainement`() = runTest {
        finishEntirely(startProgram(WorkoutType.UPPER_BODY))
        clock += 1000
        finishEntirely(startProgram(WorkoutType.LOWER_BODY))
        clock += 1000
        finishEntirely(startProgram(WorkoutType.LOWER_BODY))

        val stats = repository.observeWorkoutStats().first()
        assertEquals(3, repository.observeStarCount().first())
        assertEquals(1, stats.getValue(programId(WorkoutType.UPPER_BODY)).sessionCount)
        assertEquals(2, stats.getValue(programId(WorkoutType.LOWER_BODY)).sessionCount)
        assertEquals(clock, stats.getValue(programId(WorkoutType.LOWER_BODY)).lastDoneAt)
    }

    @Test
    fun `abandonner une seance supprime aussi ses series`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(sessionId, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 40.0, repetitions = 12)

        repository.abortSession(sessionId)

        assertNull(repository.session(sessionId))
        assertNull(repository.observeActiveSession().first())
        assertTrue(repository.observeSetsForExercise("chest_press").first().isEmpty())
    }

    @Test
    fun `la charge de reference ignore les series de la seance en cours`() = runTest {
        val previous = startProgram(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(previous, "chest_press"), "chest_press", 1, weightKg = 40.0, repetitions = 12)
        finishEntirely(previous)

        clock += 86_400_000L
        val current = startProgram(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(current, "chest_press"), "chest_press", 1, weightKg = 45.0, repetitions = 10)

        val reference = repository.observePreviousWeights(current).first()["chest_press"]
        assertEquals(40.0, reference!!, 0.001)
    }

    @Test
    fun `l historique ne contient que les seances terminees`() = runTest {
        finishEntirely(startProgram(WorkoutType.UPPER_BODY))
        clock += 1000
        startProgram(WorkoutType.LOWER_BODY)

        val history = repository.observeFinishedSessions().first()
        assertEquals(1, history.size)
        assertEquals(WorkoutType.UPPER_BODY.label, history.single().session.name)
        assertNotNull(repository.observeActiveSession().first())
    }

    // --- Réordonnancement libre ---

    @Test
    fun `l ordre choisi est applique tel quel`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val wanted = listOf("seated_row", Program.PLANK, "chest_press", Program.BIKE, "pec_deck", "lat_pulldown", Program.STOMACH_VACUUM)

        repository.applyPendingOrder(sessionId, wanted.map { stepFor(sessionId, it) })

        assertEquals(wanted, orderOf(sessionId))
    }

    @Test
    fun `remonter un exercice en tete le rend courant`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val order = orderOf(sessionId).toMutableList()
        order.add(0, order.removeAt(order.indexOf("seated_row")))

        repository.applyPendingOrder(sessionId, order.map { stepFor(sessionId, it) })

        assertEquals("seated_row", orderOf(sessionId).first())
    }

    @Test
    fun `les exercices termines restent en tete et dans leur ordre`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(sessionId, Program.BIKE), Program.BIKE, 1, durationSeconds = 300)
        val pending = orderOf(sessionId).filterNot { it == Program.BIKE }.reversed()

        repository.applyPendingOrder(sessionId, pending.map { stepFor(sessionId, it) })

        val order = orderOf(sessionId)
        assertEquals(Program.BIKE, order.first())
        assertEquals(pending, order.drop(1))
    }

    @Test
    fun `une etape absente de l ordre demande est conservee a la suite`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val partial = listOf("seated_row", "pec_deck")

        repository.applyPendingOrder(sessionId, partial.map { stepFor(sessionId, it) })

        val order = orderOf(sessionId)
        assertEquals(partial, order.take(2))
        assertEquals(7, order.size)
        assertEquals(Program.planFor(WorkoutType.UPPER_BODY).map { it.exerciseId }.toSet(), order.toSet())
    }

    @Test
    fun `un exercice repris conserve ses series deja faites`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(sessionId, "chest_press")
        repository.recordSet(chestPress, "chest_press", 1, weightKg = 45.0, repetitions = 12)
        repository.recordSet(chestPress, "chest_press", 2, weightKg = 45.0, repetitions = 11)

        val reversed = orderOf(sessionId).reversed()
        repository.applyPendingOrder(sessionId, reversed.map { stepFor(sessionId, it) })

        val sets = repository.session(sessionId)!!
            .orderedExercises
            .first { it.exerciseSession.id == chestPress }
            .sets
        assertEquals(2, sets.size)
    }

    @Test
    fun `apres reordonnancement la progression pointe sur le nouvel exercice courant`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        val order = orderOf(sessionId).toMutableList()
        order.add(0, order.removeAt(order.indexOf("lat_pulldown")))
        repository.applyPendingOrder(sessionId, order.map { stepFor(sessionId, it) })

        val session = repository.observeActiveSession().first()!!
        val progress = WorkoutProgression.compute(
            session.orderedExercises.map { StepState(it.exerciseSession.plannedSets, it.sets.size) },
        )
        assertEquals(0, progress.currentStepIndex)
        assertEquals(
            "lat_pulldown",
            session.orderedExercises[progress.currentStepIndex!!].exerciseSession.exerciseId,
        )
    }

    @Test
    fun `le reordonnancement ne vaut que pour la seance en cours`() = runTest {
        val first = startProgram(WorkoutType.UPPER_BODY)
        repository.applyPendingOrder(first, orderOf(first).reversed().map { stepFor(first, it) })
        repository.finishSession(first)

        clock += 86_400_000L
        val second = startProgram(WorkoutType.UPPER_BODY)

        assertEquals(Program.planFor(WorkoutType.UPPER_BODY).map { it.exerciseId }, orderOf(second))
    }

    @Test
    fun `les positions restent contigues apres plusieurs reordonnancements`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        repeat(3) {
            repository.applyPendingOrder(
                sessionId,
                orderOf(sessionId).reversed().map { stepFor(sessionId, it) },
            )
        }

        val positions = repository.session(sessionId)!!
            .orderedExercises
            .map { it.exerciseSession.position }
        assertEquals((0..6).toList(), positions)
    }

    // --- Entraînements personnalisés ---

    @Test
    fun `un entrainement personnalise conserve ordre et series`() = runTest {
        val id = repository.saveCustomWorkout(
            workoutId = null,
            name = "  Full body ",
            steps = listOf(PlannedStep("leg_press", 5), PlannedStep("chest_press", 2), PlannedStep(Program.PLANK, 3)),
        )

        val saved = repository.customWorkout(id)!!
        assertEquals("Full body", saved.workout.name)
        assertEquals(
            listOf("leg_press" to 5, "chest_press" to 2, Program.PLANK to 3),
            saved.orderedExercises.map { it.exerciseId to it.plannedSets },
        )
    }

    @Test
    fun `modifier un entrainement remplace son contenu`() = runTest {
        val id = repository.saveCustomWorkout(null, "A", listOf(PlannedStep("leg_press", 4), PlannedStep("lying_leg_curl", 3)))
        repository.saveCustomWorkout(id, "B", listOf(PlannedStep("standing_calf_raise", 2)))

        val saved = repository.customWorkout(id)!!
        assertEquals("B", saved.workout.name)
        assertEquals(listOf("standing_calf_raise"), saved.orderedExercises.map { it.exerciseId })
        // Les deux programmes insérés au premier lancement, plus celui-ci.
        assertEquals(3, repository.observeCustomWorkouts().first().size)
    }

    @Test
    fun `une seance personnalisee suit le plan de l entrainement`() = runTest {
        val id = repository.saveCustomWorkout(null, "Jambes", listOf(PlannedStep("lying_leg_curl", 5), PlannedStep(Program.BIKE, 1)))

        val sessionId = repository.startSession(id)

        val session = repository.session(sessionId)!!
        assertEquals(WorkoutType.CUSTOM, session.session.type)
        assertEquals("Jambes", session.session.name)
        assertEquals(listOf("lying_leg_curl", Program.BIKE), orderOf(sessionId))
        assertEquals(listOf(5, 1), session.orderedExercises.map { it.exerciseSession.plannedSets })
    }

    @Test
    fun `une seance personnalisee reprend la derniere charge connue`() = runTest {
        val upper = startProgram(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(upper, "chest_press"), "chest_press", 1, weightKg = 42.5, repetitions = 10)
        repository.finishSession(upper)

        val id = repository.saveCustomWorkout(null, "Pecs", listOf(PlannedStep("chest_press", 3)))
        val sessionId = repository.startSession(id)

        val step = repository.session(sessionId)!!.orderedExercises.single()
        assertEquals(42.5, step.exerciseSession.plannedWeightKg!!, 0.0)
    }

    @Test
    fun `supprimer un entrainement garde ses seances dans l historique`() = runTest {
        val id = repository.saveCustomWorkout(null, "Éphémère", listOf(PlannedStep("pec_deck", 3)))
        val sessionId = repository.startSession(id)
        repository.finishSession(sessionId)

        repository.deleteCustomWorkout(id)

        assertNull(repository.customWorkout(id))
        val history = repository.observeFinishedSessions().first()
        assertEquals("Éphémère", history.single().session.name)
        assertEquals(1, repository.observeStarCount().first())
    }

    @Test
    fun `lancer un entrainement pendant une seance renvoie la seance en cours`() = runTest {
        val active = startProgram(WorkoutType.LOWER_BODY)
        val id = repository.saveCustomWorkout(null, "Autre", listOf(PlannedStep("pec_deck", 3)))

        assertEquals(active, repository.startSession(id))
    }

    @Test
    fun `les cibles personnalisees sont reportees sur la seance`() = runTest {
        val id = repository.saveCustomWorkout(
            null,
            "Cibles",
            listOf(
                PlannedStep("chest_press", 3, targetRepsMin = 5, targetRepsMax = 6),
                PlannedStep(Program.PLANK, 2, targetDurationSeconds = 90),
                PlannedStep("pec_deck", 3),
            ),
        )

        val steps = repository.session(repository.startSession(id))!!
            .orderedExercises
            .map { it.exerciseSession }
        assertEquals(5 to 6, steps[0].targetRepsMin to steps[0].targetRepsMax)
        assertEquals(90, steps[1].targetDurationSeconds)
        // Sans réglage, l'étape s'en remet au catalogue.
        assertNull(steps[2].targetRepsMin)
        assertNull(steps[2].targetRepsMax)
    }

    @Test
    fun `le programme est installe comme deux entrainements ordinaires`() = runTest {
        val workouts = repository.observeCustomWorkouts().first()

        assertEquals(Program.types.map { it.label }, workouts.map { it.workout.name })
        Program.types.forEach { type ->
            val workout = workouts.first { it.workout.name == type.label }
            assertEquals(
                Program.planFor(type).map { it.exerciseId to it.plannedSets },
                workout.orderedExercises.map { it.exerciseId to it.plannedSets },
            )
        }
    }

    @Test
    fun `un entrainement du programme se modifie comme les autres`() = runTest {
        val upper = programId(WorkoutType.UPPER_BODY)
        repository.saveCustomWorkout(upper, "Haut du corps", listOf(PlannedStep("pec_deck", 2)))

        val sessionId = repository.startSession(upper)

        assertEquals(listOf("pec_deck"), orderOf(sessionId))
        assertEquals("Haut du corps", repository.session(sessionId)!!.session.name)
    }

    @Test
    fun `un entrainement du programme supprime ne revient pas`() = runTest {
        repository.deleteCustomWorkout(programId(WorkoutType.LOWER_BODY))
        repository.ensureSeeded()

        assertEquals(
            listOf(WorkoutType.UPPER_BODY.label),
            repository.observeCustomWorkouts().first().map { it.workout.name },
        )
    }

    @Test
    fun `une seance du programme recoit les cibles et le repos de son entrainement`() = runTest {
        val steps = repository.session(startProgram(WorkoutType.UPPER_BODY))!!
            .orderedExercises
            .associateBy { it.exerciseSession.exerciseId }
            .mapValues { it.value.exerciseSession }
        val chest = steps.getValue("chest_press")
        assertEquals(listOf(4, 8, 12, 60), listOf(chest.plannedSets, chest.targetRepsMin, chest.targetRepsMax, chest.restSeconds))
        val bike = steps.getValue(Program.BIKE)
        assertEquals(300, bike.targetDurationSeconds)
        assertEquals(0, bike.restSeconds)
    }

    @Test
    fun `le repos choisi dans l entrainement est reporte sur la seance`() = runTest {
        val id = repository.saveCustomWorkout(null, "Repos", listOf(PlannedStep("pec_deck", 3, 10, 12, restSeconds = 90)))
        val step = repository.session(repository.startSession(id))!!.orderedExercises.single().exerciseSession
        assertEquals(90, step.restSeconds)
    }

    // --- Exercices créés ---

    @Test
    fun `un exercice cree survit au re semis du catalogue`() = runTest {
        val custom = ExerciseEntity(
            id = "custom_1",
            name = "  Développé incliné ",
            kind = ExerciseKind.WEIGHTED_REPS,
            isCustom = true,
        )
        repository.saveCustomExercise(custom)
        repository.ensureSeeded()

        val saved = repository.exercise("custom_1")!!
        assertEquals("Développé incliné", saved.name)
        assertEquals(Program.exercises.size + 1, database.exerciseDao().count())
    }

    @Test
    fun `un exercice cree s utilise dans un entrainement`() = runTest {
        repository.saveCustomExercise(
            ExerciseEntity("custom_2", "Gainage latéral", ExerciseKind.TIMED, isCustom = true),
        )
        val id = repository.saveCustomWorkout(null, "Core", listOf(PlannedStep("custom_2", 2, targetDurationSeconds = 45)))

        val sessionId = repository.startSession(id)

        assertEquals(listOf("custom_2"), orderOf(sessionId))
    }

    @Test
    fun `supprimer un exercice le retire des entrainements et du catalogue mais garde l historique`() = runTest {
        repository.saveCustomExercise(ExerciseEntity("custom_3", "Rowing", ExerciseKind.WEIGHTED_REPS, isCustom = true))
        val a = repository.saveCustomWorkout(null, "A", listOf(PlannedStep("custom_3", 3, 8, 12), PlannedStep("pec_deck", 3, 10, 15)))
        repository.saveCustomWorkout(null, "B", listOf(PlannedStep("custom_3", 2, 8, 12)))
        val sessionId = repository.startSession(a)
        repository.recordSet(stepFor(sessionId, "custom_3"), "custom_3", 1, weightKg = 30.0, repetitions = 10)
        repository.finishSession(sessionId)

        assertEquals(listOf("A", "B"), repository.workoutsUsing("custom_3"))
        repository.deleteCustomExercise("custom_3")

        assertEquals(listOf("pec_deck"), repository.customWorkout(a)!!.orderedExercises.map { it.exerciseId })
        assertTrue(repository.observeCatalogue().first().none { it.id == "custom_3" })
        // L'historique garde l'exercice (archivé) et ses séries.
        assertEquals("Rowing", repository.exercise("custom_3")!!.name)
        assertTrue(repository.exercise("custom_3")!!.isArchived)
        assertEquals(1, repository.observeSetsForExercise("custom_3").first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un exercice de l application ne se supprime pas`() = runTest {
        repository.deleteCustomExercise("chest_press")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un exercice du programme ne se modifie pas`() = runTest {
        val chestPress = repository.exercise("chest_press")!!
        repository.saveCustomExercise(chestPress.copy(name = "Autre"))
    }

    // --- Seules les séances terminées comptent ---

    @Test
    fun `une seance en cours ne compte dans aucune statistique avant d etre terminee`() = runTest {
        val sessionId = startProgram(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(sessionId, "pec_deck"), "pec_deck", 1, weightKg = 30.0, repetitions = 12)

        assertTrue(repository.observeRecentWeightedSets().first().isEmpty())
        assertTrue(repository.observeSetsForExercise("pec_deck").first().isEmpty())
        assertNull(repository.observeLastWeight("pec_deck").first())

        repository.finishSession(sessionId)

        assertEquals(1, repository.observeRecentWeightedSets().first().size)
        assertEquals(1, repository.observeSetsForExercise("pec_deck").first().size)
        assertEquals(30.0, repository.observeLastWeight("pec_deck").first()!!, 0.0)
    }

    @Test
    fun `une seance abandonnee ne laisse aucune trace meme sans cles etrangeres actives`() = runTest {
        // Base sans le callback qui active les clés étrangères : la cascade ne jouerait pas.
        val bare = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            bare.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = OFF")
            val repo = WorkoutRepository(bare.exerciseDao(), bare.workoutDao(), bare.customWorkoutDao()) { clock }
            repo.ensureSeeded()
            val workout = repo.saveCustomWorkout(null, "Test", listOf(PlannedStep("pec_deck", 3, 10, 15)))
            val sessionId = repo.startSession(workout)
            val step = repo.session(sessionId)!!.orderedExercises.single().exerciseSession.id
            repo.recordSet(step, "pec_deck", 1, weightKg = 30.0, repetitions = 12)

            repo.abortSession(sessionId)

            val db = bare.openHelper.readableDatabase
            fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
            assertEquals(0, count("workout_session"))
            assertEquals(0, count("exercise_session"))
            assertEquals(0, count("set_result"))
        } finally {
            bare.close()
        }
    }

    // --- Langue ---

    private val english = LocalizedNames(
        exercises = mapOf("seated_row" to "Seated row"),
        programs = mapOf(WorkoutType.UPPER_BODY to "Upper body", WorkoutType.LOWER_BODY to "Lower body"),
        knownProgramNames = mapOf(
            WorkoutType.UPPER_BODY to listOf("Upper body", "Haut du corps"),
            WorkoutType.LOWER_BODY to listOf("Lower body", "Bas du corps"),
        ),
    )

    @Test
    fun `changer de langue renomme le contenu de l app sans toucher a celui de l utilisateur`() = runTest {
        finishEntirely(startProgram(WorkoutType.UPPER_BODY))
        // Le programme « Bas du corps » a été renommé par l'utilisateur : il ne doit pas bouger.
        val lower = programId(WorkoutType.LOWER_BODY)
        repository.saveCustomWorkout(lower, "Jambes", listOf(PlannedStep("leg_press", 4, 8, 12)))
        repository.saveCustomExercise(ExerciseEntity("custom_9", "Tirage horizontal", ExerciseKind.WEIGHTED_REPS, isCustom = true))

        repository.applyLocalizedNames(english)

        val names = repository.observeCustomWorkouts().first().map { it.workout.name }
        assertEquals(listOf("Upper body", "Jambes"), names)
        assertEquals("Seated row", repository.exercise("seated_row")!!.name)
        // Un exercice de l'utilisateur au même nom n'est pas renommé.
        assertEquals("Tirage horizontal", repository.exercise("custom_9")!!.name)
        // Les séances passées suivent, pour que l'historique soit dans la même langue.
        assertEquals("Upper body", repository.observeFinishedSessions().first().single().session.name)
    }

    @Test
    fun `une installation ancienne recoit les details du catalogue`() = runTest {
        // Un exercice semé par une version précédente : ni famille, ni muscles, ni matériel.
        database.exerciseDao().upsert(ExerciseEntity("chest_press", "Chest Press", ExerciseKind.WEIGHTED_REPS))

        repository.ensureSeeded()

        val chest = repository.exercise("chest_press")!!
        assertEquals(ExerciseCategory.CHEST, chest.category)
        assertEquals(listOf(Muscle.TRICEPS, Muscle.FRONT_DELTS), chest.secondaryMuscles)
        assertEquals(Equipment.MACHINE, chest.equipment)
        // Le nom, lui, relève de la langue du téléphone : il n'est pas touché ici.
        assertEquals("Chest Press", chest.name)
    }

    @Test
    fun `les details d un exercice de l utilisateur ne sont jamais ecrases`() = runTest {
        repository.saveCustomExercise(ExerciseEntity("custom_1", "Tirage maison", ExerciseKind.WEIGHTED_REPS, isCustom = true))
        repository.ensureSeeded()
        assertNull(repository.exercise("custom_1")!!.category)
    }

    @Test
    fun `une cible en distance va de l entrainement a la seance, et la distance a la serie`() = runTest {
        val workoutId = repository.saveCustomWorkout(
            workoutId = null,
            name = "Portés",
            steps = listOf(PlannedStep("farmer_carry", plannedSets = 2, targetDistanceMeters = 40)),
        )
        val sessionId = repository.startSession(workoutId)
        val step = repository.session(sessionId)!!.orderedExercises.single().exerciseSession
        assertEquals(40, step.targetDistanceMeters)

        repository.recordSet(step.id, "farmer_carry", setNumber = 1, weightKg = 24.0, distanceMeters = 40.0)
        repository.finishSession(sessionId)

        val set = repository.observeSetsForExercise("farmer_carry").first().single()
        assertEquals(40.0, set.distanceMeters!!, 0.0)
        assertEquals(24.0, set.weightKg!!, 0.0)
        // La séance suivante connaît la distance de celle-ci.
        assertEquals(mapOf("farmer_carry" to 40.0), repository.observePreviousDistances(-1).first())
    }

    @Test
    fun `un exercice renomme emporte son historique sous son nouvel identifiant`() = runTest {
        // Une installation d'avant le catalogue : « leg_curl », avec une séance faite et un entraînement.
        database.exerciseDao().upsert(ExerciseEntity("leg_curl", "Leg Curl", ExerciseKind.WEIGHTED_REPS))
        val workoutId = repository.saveCustomWorkout(null, "Jambes", listOf(PlannedStep("leg_curl", plannedSets = 3)))
        val sessionId = repository.startSession(workoutId)
        val step = repository.session(sessionId)!!.orderedExercises.single().exerciseSession
        repository.recordSet(step.id, "leg_curl", setNumber = 1, weightKg = 30.0, repetitions = 12)
        repository.finishSession(sessionId)

        repository.ensureSeeded()

        assertNull(repository.exercise("leg_curl"))
        assertEquals(30.0, repository.observeSetsForExercise("lying_leg_curl").first().single().weightKg!!, 0.0)
        assertEquals("lying_leg_curl", repository.session(sessionId)!!.orderedExercises.single().exerciseSession.exerciseId)
        assertEquals("lying_leg_curl", repository.customWorkout(workoutId)!!.exercises.single().exerciseId)
        // Rejouer le démarrage ne change plus rien.
        repository.ensureSeeded()
        assertEquals(Program.exercises.size, database.exerciseDao().count())
    }

    private suspend fun programId(type: WorkoutType): Long =
        repository.observeCustomWorkouts().first()
            .first { it.workout.name == type.label }
            .workout
            .id

    private suspend fun startProgram(type: WorkoutType): Long =
        repository.startSession(programId(type))

    private suspend fun orderOf(sessionId: Long): List<String> =
        repository.session(sessionId)!!
            .orderedExercises
            .map { it.exerciseSession.exerciseId }

    private suspend fun stepFor(sessionId: Long, exerciseId: String): Long =
        repository.session(sessionId)!!
            .orderedExercises
            .first { it.exerciseSession.exerciseId == exerciseId }
            .exerciseSession
            .id

    private suspend fun plannedSetsOf(sessionId: Long, exerciseSessionId: Long): Int =
        repository.session(sessionId)!!
            .orderedExercises
            .first { it.exerciseSession.id == exerciseSessionId }
            .exerciseSession
            .plannedSets

    private suspend fun finishEntirely(sessionId: Long) {
        repository.finishSession(sessionId)
    }
}

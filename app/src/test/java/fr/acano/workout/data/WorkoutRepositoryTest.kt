package fr.acano.workout.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.Program
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
        ).allowMainThreadQueries().build()
        repository = WorkoutRepository(database.exerciseDao(), database.workoutDao()) { clock }
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
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        val session = repository.session(sessionId)!!

        assertEquals(7, session.orderedExercises.size)
        assertEquals(
            Program.planFor(WorkoutType.UPPER_BODY),
            session.orderedExercises.map { it.exerciseSession.exerciseId },
        )
    }

    @Test
    fun `une seule seance peut etre en cours a la fois`() = runTest {
        val first = repository.startSession(WorkoutType.UPPER_BODY)
        val second = repository.startSession(WorkoutType.LOWER_BODY)

        assertEquals(first, second)
    }

    @Test
    fun `la charge de la seance precedente est pre remplie`() = runTest {
        val first = repository.startSession(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(first, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 42.5, repetitions = 10)
        finishEntirely(first)

        clock += 86_400_000L
        val second = repository.startSession(WorkoutType.UPPER_BODY)
        val session = repository.session(second)!!
        val planned = session.orderedExercises
            .first { it.exerciseSession.exerciseId == "chest_press" }
            .exerciseSession
            .plannedWeightKg

        assertEquals(42.5, planned!!, 0.001)
    }

    @Test
    fun `la premiere seance n a aucune charge pre remplie`() = runTest {
        val sessionId = repository.startSession(WorkoutType.LOWER_BODY)
        val session = repository.session(sessionId)!!

        assertTrue(session.orderedExercises.all { it.exerciseSession.plannedWeightKg == null })
    }

    @Test
    fun `une seance interrompue est reprise sur la serie exacte`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
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
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
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
    fun `terminer une seance attribue exactement une etoile`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        repository.finishSession(sessionId)
        repository.finishSession(sessionId)

        assertEquals(1, repository.observeStarCount().first())
    }

    @Test
    fun `une seance en cours ne rapporte pas d etoile`() = runTest {
        repository.startSession(WorkoutType.UPPER_BODY)

        assertEquals(0, repository.observeStarCount().first())
    }

    @Test
    fun `les etoiles sont comptees par type de seance`() = runTest {
        finishEntirely(repository.startSession(WorkoutType.UPPER_BODY))
        clock += 1000
        finishEntirely(repository.startSession(WorkoutType.LOWER_BODY))
        clock += 1000
        finishEntirely(repository.startSession(WorkoutType.LOWER_BODY))

        assertEquals(3, repository.observeStarCount().first())
        assertEquals(1, repository.observeStarCount(WorkoutType.UPPER_BODY).first())
        assertEquals(2, repository.observeStarCount(WorkoutType.LOWER_BODY).first())
    }

    @Test
    fun `abandonner une seance supprime aussi ses series`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        val chestPress = stepFor(sessionId, "chest_press")
        repository.recordSet(chestPress, "chest_press", setNumber = 1, weightKg = 40.0, repetitions = 12)

        repository.abortSession(sessionId)

        assertNull(repository.session(sessionId))
        assertNull(repository.observeActiveSession().first())
        assertTrue(repository.observeSetsForExercise("chest_press").first().isEmpty())
    }

    @Test
    fun `la charge de reference ignore les series de la seance en cours`() = runTest {
        val previous = repository.startSession(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(previous, "chest_press"), "chest_press", 1, weightKg = 40.0, repetitions = 12)
        finishEntirely(previous)

        clock += 86_400_000L
        val current = repository.startSession(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(current, "chest_press"), "chest_press", 1, weightKg = 45.0, repetitions = 10)

        val reference = repository.observePreviousWeights(current).first()["chest_press"]
        assertEquals(40.0, reference!!, 0.001)
    }

    @Test
    fun `l historique ne contient que les seances terminees`() = runTest {
        finishEntirely(repository.startSession(WorkoutType.UPPER_BODY))
        clock += 1000
        repository.startSession(WorkoutType.LOWER_BODY)

        val history = repository.observeFinishedSessions().first()
        assertEquals(1, history.size)
        assertEquals(WorkoutType.UPPER_BODY, history.single().session.type)
        assertNotNull(repository.observeActiveSession().first())
    }

    // --- Réordonnancement libre ---

    @Test
    fun `l ordre choisi est applique tel quel`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        val wanted = listOf("seated_row", Program.PLANK, "chest_press", Program.BIKE, "pec_deck", "lat_pulldown", Program.STOMACH_VACUUM)

        repository.applyPendingOrder(sessionId, wanted.map { stepFor(sessionId, it) })

        assertEquals(wanted, orderOf(sessionId))
    }

    @Test
    fun `remonter un exercice en tete le rend courant`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        val order = orderOf(sessionId).toMutableList()
        order.add(0, order.removeAt(order.indexOf("seated_row")))

        repository.applyPendingOrder(sessionId, order.map { stepFor(sessionId, it) })

        assertEquals("seated_row", orderOf(sessionId).first())
    }

    @Test
    fun `les exercices termines restent en tete et dans leur ordre`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        repository.recordSet(stepFor(sessionId, Program.BIKE), Program.BIKE, 1, durationSeconds = 300)
        val pending = orderOf(sessionId).filterNot { it == Program.BIKE }.reversed()

        repository.applyPendingOrder(sessionId, pending.map { stepFor(sessionId, it) })

        val order = orderOf(sessionId)
        assertEquals(Program.BIKE, order.first())
        assertEquals(pending, order.drop(1))
    }

    @Test
    fun `une etape absente de l ordre demande est conservee a la suite`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
        val partial = listOf("seated_row", "pec_deck")

        repository.applyPendingOrder(sessionId, partial.map { stepFor(sessionId, it) })

        val order = orderOf(sessionId)
        assertEquals(partial, order.take(2))
        assertEquals(7, order.size)
        assertEquals(Program.planFor(WorkoutType.UPPER_BODY).toSet(), order.toSet())
    }

    @Test
    fun `un exercice repris conserve ses series deja faites`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
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
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
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
        val first = repository.startSession(WorkoutType.UPPER_BODY)
        repository.applyPendingOrder(first, orderOf(first).reversed().map { stepFor(first, it) })
        repository.finishSession(first)

        clock += 86_400_000L
        val second = repository.startSession(WorkoutType.UPPER_BODY)

        assertEquals(Program.planFor(WorkoutType.UPPER_BODY), orderOf(second))
    }

    @Test
    fun `les positions restent contigues apres plusieurs reordonnancements`() = runTest {
        val sessionId = repository.startSession(WorkoutType.UPPER_BODY)
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

    private suspend fun finishEntirely(sessionId: Long) {
        repository.finishSession(sessionId)
    }
}

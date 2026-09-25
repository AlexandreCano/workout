package fr.acano.workout

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vérifie sur un vrai appareil que la base s'ouvre, se sème et démarre une séance.
 * Les invariants métier sont couverts par les tests unitaires ; celui-ci valide
 * le SQLite embarqué et le schéma généré par Room.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseTest {

    @Test
    fun ouvre_seme_et_demarre_une_seance() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("workout.db")
        val database = WorkoutDatabase.build(context)
        try {
            val repository = WorkoutRepository(database.exerciseDao(), database.workoutDao(), database.customWorkoutDao())
            repository.ensureSeeded()
            assertEquals(Program.exercises.size, database.exerciseDao().count())

            val upper = repository.observeCustomWorkouts().first()
                .first { it.workout.name == WorkoutType.UPPER_BODY.label }
            val sessionId = repository.startSession(upper.workout.id)
            assertEquals(7, repository.session(sessionId)!!.orderedExercises.size)
        } finally {
            database.close()
            context.deleteDatabase("workout.db")
        }
    }
}

package fr.acano.workout.data

import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramTest {

    @Test
    fun `chaque exercice planifie existe dans le catalogue`() {
        val catalogue = Program.exercises.map { it.id }.toSet()

        WorkoutType.entries.forEach { type ->
            Program.planFor(type).forEach { exerciseId ->
                assertTrue("$exerciseId absent du catalogue", exerciseId in catalogue)
            }
        }
    }

    @Test
    fun `les deux seances commencent par l echauffement velo de cinq minutes`() {
        WorkoutType.entries.forEach { type ->
            assertEquals(Program.BIKE, Program.planFor(type).first())
        }
        val bike = Program.exercises.first { it.id == Program.BIKE }
        assertEquals(300, bike.targetDurationSeconds)
        assertEquals(0, bike.restSeconds)
    }

    @Test
    fun `les deux seances finissent par planche puis stomach vacuum`() {
        WorkoutType.entries.forEach { type ->
            val plan = Program.planFor(type)
            assertEquals(Program.STOMACH_VACUUM, plan.last())
            assertEquals(Program.PLANK, plan[plan.lastIndex - 1])
        }
    }

    @Test
    fun `la planche est quatre series d une minute`() {
        val plank = Program.exercises.first { it.id == Program.PLANK }

        assertEquals(ExerciseKind.TIMED, plank.kind)
        assertEquals(4, plank.plannedSets)
        assertEquals(60, plank.targetDurationSeconds)
        assertEquals(60, plank.restSeconds)
    }

    @Test
    fun `le stomach vacuum vise trois a cinq repetitions sans charge`() {
        val vacuum = Program.exercises.first { it.id == Program.STOMACH_VACUUM }

        assertEquals(ExerciseKind.REPS_ONLY, vacuum.kind)
        assertEquals(3, vacuum.targetRepsMin)
        assertEquals(5, vacuum.targetRepsMax)
    }

    @Test
    fun `chaque seance compte sept etapes`() {
        WorkoutType.entries.forEach { type ->
            assertEquals(7, Program.planFor(type).size)
        }
    }

    @Test
    fun `le chest press est bien quatre series de huit a douze`() {
        val chestPress = Program.exercises.first { it.id == "chest_press" }

        assertEquals(ExerciseKind.WEIGHTED_REPS, chestPress.kind)
        assertEquals(4, chestPress.plannedSets)
        assertEquals(8, chestPress.targetRepsMin)
        assertEquals(12, chestPress.targetRepsMax)
    }

    @Test
    fun `aucun identifiant d exercice n est duplique`() {
        val ids = Program.exercises.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}

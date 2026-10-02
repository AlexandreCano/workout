package fr.acano.workout.data

import fr.acano.workout.data.seed.Program
import fr.acano.workout.domain.ExerciseKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramTest {

    private fun step(type: fr.acano.workout.domain.WorkoutType, exerciseId: String) =
        Program.planFor(type).first { it.exerciseId == exerciseId }

    @Test
    fun `chaque exercice planifie existe dans le catalogue`() {
        val catalogue = Program.exercises.map { it.id }.toSet()
        Program.types.forEach { type ->
            Program.planFor(type).forEach { step ->
                assertTrue("${step.exerciseId} absent du catalogue", step.exerciseId in catalogue)
            }
        }
    }

    @Test
    fun `les deux seances commencent par l echauffement velo de cinq minutes`() {
        Program.types.forEach { type ->
            val bike = Program.planFor(type).first()
            assertEquals(Program.BIKE, bike.exerciseId)
            assertEquals(300, bike.targetDurationSeconds)
            assertEquals(0, bike.restSeconds)
        }
    }

    @Test
    fun `les deux seances finissent par planche puis stomach vacuum`() {
        Program.types.forEach { type ->
            val plan = Program.planFor(type).map { it.exerciseId }
            assertEquals(Program.STOMACH_VACUUM, plan.last())
            assertEquals(Program.PLANK, plan[plan.lastIndex - 1])
        }
    }

    @Test
    fun `la planche est quatre series d une minute`() {
        val plank = step(Program.types.first(), Program.PLANK)
        assertEquals(ExerciseKind.TIMED, Program.exercises.first { it.id == Program.PLANK }.kind)
        assertEquals(4, plank.plannedSets)
        assertEquals(60, plank.targetDurationSeconds)
        assertEquals(60, plank.restSeconds)
        assertNull(plank.targetRepsMin)
    }

    @Test
    fun `le stomach vacuum vise trois a cinq repetitions sans charge`() {
        val vacuum = step(Program.types.first(), Program.STOMACH_VACUUM)
        assertEquals(ExerciseKind.REPS_ONLY, Program.exercises.first { it.id == Program.STOMACH_VACUUM }.kind)
        assertEquals(3, vacuum.targetRepsMin)
        assertEquals(5, vacuum.targetRepsMax)
    }

    @Test
    fun `chaque seance compte sept etapes`() {
        Program.types.forEach { type -> assertEquals(7, Program.planFor(type).size) }
    }

    @Test
    fun `le chest press est bien quatre series de huit a douze`() {
        val chestPress = step(Program.types.first(), "chest_press")
        assertEquals(4, chestPress.plannedSets)
        assertEquals(8, chestPress.targetRepsMin)
        assertEquals(12, chestPress.targetRepsMax)
    }

    @Test
    fun `un exercice ajoute a un entrainement recoit des reglages selon son type`() {
        // Un exercice du programme reprend ses réglages du programme…
        assertEquals(4, Program.defaultStepFor("chest_press", ExerciseKind.WEIGHTED_REPS).plannedSets)
        // … un exercice inconnu, des valeurs courantes pour son type.
        val timed = Program.defaultStepFor("custom_1", ExerciseKind.TIMED)
        assertEquals(60, timed.targetDurationSeconds)
        assertNull(timed.targetRepsMin)
        val reps = Program.defaultStepFor("custom_2", ExerciseKind.WEIGHTED_REPS)
        assertEquals(8 to 12, reps.targetRepsMin to reps.targetRepsMax)
        assertNull(reps.targetDurationSeconds)
    }

    @Test
    fun `le cardio et les exercices en distance recoivent des reglages adaptes`() {
        // Tapis : une série de dix minutes, sans repos ni reps.
        val treadmill = Program.defaultStepFor("treadmill", ExerciseKind.TIMED_DISTANCE)
        assertEquals(1 to 600, treadmill.plannedSets to treadmill.targetDurationSeconds)
        assertEquals(0, treadmill.restSeconds)
        // Un exercice chronométré de la famille cardio aussi.
        assertEquals(600, Program.defaultStepFor("elliptical", ExerciseKind.TIMED).targetDurationSeconds)
        // Farmer carry : une distance à parcourir, pas une durée.
        val carry = Program.defaultStepFor("farmer_carry", ExerciseKind.WEIGHTED_DISTANCE)
        assertEquals(30, carry.targetDistanceMeters)
        assertNull(carry.targetDurationSeconds)
    }

    @Test
    fun `aucun identifiant d exercice n est duplique`() {
        val ids = Program.exercises.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}

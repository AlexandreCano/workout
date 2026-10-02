package fr.acano.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La progression est la brique dont dépend la reprise de séance :
 * elle est testée exhaustivement, y compris sur les cas limites.
 */
class WorkoutProgressionTest {

    @Test
    fun `seance neuve commence a la premiere serie du premier exercice`() {
        val progress = WorkoutProgression.compute(
            listOf(StepState(1, 0), StepState(4, 0), StepState(3, 0)),
        )

        assertEquals(0, progress.currentStepIndex)
        assertEquals(1, progress.currentSetNumber)
        assertEquals(0, progress.completedSteps)
        assertEquals(3, progress.totalSteps)
        assertFalse(progress.isFinished)
    }

    @Test
    fun `la serie courante suit le nombre de series deja validees`() {
        val progress = WorkoutProgression.compute(
            listOf(StepState(1, 1), StepState(4, 2)),
        )

        assertEquals(1, progress.currentStepIndex)
        assertEquals(3, progress.currentSetNumber)
        assertEquals(1, progress.completedSteps)
    }

    @Test
    fun `on passe a l exercice suivant quand toutes les series sont faites`() {
        val progress = WorkoutProgression.compute(
            listOf(StepState(4, 4), StepState(3, 0)),
        )

        assertEquals(1, progress.currentStepIndex)
        assertEquals(1, progress.currentSetNumber)
    }

    @Test
    fun `la seance est terminee quand toutes les etapes sont completes`() {
        val progress = WorkoutProgression.compute(
            listOf(StepState(1, 1), StepState(4, 4), StepState(1, 1)),
        )

        assertNull(progress.currentStepIndex)
        assertTrue(progress.isFinished)
        assertEquals(3, progress.completedSteps)
    }

    @Test
    fun `une etape avec plus de series que prevu est consideree comme complete`() {
        val progress = WorkoutProgression.compute(listOf(StepState(3, 5), StepState(2, 0)))

        assertEquals(1, progress.currentStepIndex)
    }

    @Test
    fun `une etape passee est consideree comme terminee meme sans serie`() {
        val progress = WorkoutProgression.compute(
            listOf(StepState(1, 1), StepState(4, 0, isSkipped = true), StepState(3, 0)),
        )

        assertEquals(2, progress.currentStepIndex)
        assertEquals(1, progress.currentSetNumber)
        assertEquals(2, progress.completedSteps)
    }

    @Test
    fun `passer la derniere etape termine la seance`() {
        val progress = WorkoutProgression.compute(listOf(StepState(1, 1), StepState(3, 1, isSkipped = true)))

        assertTrue(progress.isFinished)
    }

    @Test
    fun `une seance sans etape n est pas consideree comme terminee`() {
        val progress = WorkoutProgression.compute(emptyList())

        assertNull(progress.currentStepIndex)
        assertFalse(progress.isFinished)
    }

    /** Reprise après fermeture de l'application : l'état se recalcule sans rien avoir stocké. */
    @Test
    fun `la reprise retombe exactement sur la serie interrompue`() {
        val steps = listOf(StepState(1, 1), StepState(4, 2), StepState(3, 0))

        val beforeKill = WorkoutProgression.compute(steps)
        val afterRestart = WorkoutProgression.compute(steps)

        assertEquals(beforeKill, afterRestart)
        assertEquals(1, afterRestart.currentStepIndex)
        assertEquals(3, afterRestart.currentSetNumber)
    }
}

package fr.acano.workout.domain

import fr.acano.workout.data.db.entity.SetResultEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetStatsTest {

    private fun set(step: Long, weight: Double?, reps: Int?, duration: Int? = null, n: Int = 1) =
        SetResultEntity(0, step, "x", n, weight, reps, duration, 0)

    @Test
    fun `la meilleure serie est la plus lourde puis celle aux plus de repetitions`() {
        assertEquals(3, bestSetIndex(listOf(set(1, 40.0, 12), set(1, 42.5, 10), set(1, 42.5, 9), set(1, 45.0, 8))))
        assertEquals(0, bestSetIndex(listOf(set(1, 27.5, 12), set(1, 27.5, 11), set(1, 27.5, 10))))
    }

    @Test
    fun `sans charge on retient le plus de repetitions ou la plus longue duree`() {
        assertEquals(1, bestSetIndex(listOf(set(1, null, 8), set(1, null, 10))))
        assertEquals(0, bestSetIndex(listOf(set(1, null, null, 60), set(1, null, null, 45))))
    }

    @Test
    fun `aucune serie n est mise en avant si elles sont identiques ou seules`() {
        assertNull(bestSetIndex(listOf(set(1, 35.0, 12), set(1, 35.0, 12), set(1, 35.0, 12))))
        assertNull(bestSetIndex(listOf(set(1, 35.0, 12))))
    }

    @Test
    fun `la charge totale additionne charge fois repetitions`() {
        assertEquals(40.0 * 12 + 42.5 * 10, volumeKg(listOf(set(1, 40.0, 12), set(1, 42.5, 10), set(1, null, null, 60))), 1e-9)
    }

    @Test
    fun `l ecart se calcule sur la charge la plus lourde de la seance precedente`() {
        val today = listOf(set(9, 40.0, 12), set(9, 45.0, 8))
        // Du plus récent au plus ancien : la séance 8 est la précédente, la 7 ne compte pas.
        val earlier = listOf(set(8, 40.0, 10), set(8, 42.5, 8), set(7, 50.0, 5))
        assertEquals(SessionComparison.Weight(2.5), compareWithPreviousSession(today, earlier))
    }

    @Test
    fun `premiere fois ou exercice sans charge`() {
        assertEquals(SessionComparison.FirstTime, compareWithPreviousSession(listOf(set(9, 40.0, 12)), emptyList()))
        assertNull(compareWithPreviousSession(listOf(set(9, null, 12)), listOf(set(8, null, 10))))
    }

    @Test
    fun `la charge de la derniere seance est sa serie la plus lourde`() {
        // Du plus récent au plus ancien : la séance 9 est la dernière.
        val sets = listOf(set(9, 40.5, 9), set(9, 42.5, 10), set(9, 40.0, 12), set(8, 50.0, 5))
        assertEquals(42.5, latestSessionBest(sets)!!, 0.0)
        assertNull(latestSessionBest(listOf(set(9, null, 12))))
        assertNull(latestSessionBest(emptyList()))
    }
}

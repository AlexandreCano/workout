package fr.acano.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {

    @Test
    fun `le pas grandit avec la distance`() {
        assertEquals(35.0, stepDistance(30.0, up = true), 0.0)
        assertEquals(450.0, stepDistance(400.0, up = true), 0.0)
        assertEquals(2100.0, stepDistance(2000.0, up = true), 0.0)
        assertEquals(10_500.0, stepDistance(10_000.0, up = true), 0.0)
    }

    @Test
    fun `en descendant on reste sur la grille de la zone quittee`() {
        assertEquals(950.0, stepDistance(1000.0, up = false), 0.0)
        assertEquals(95.0, stepDistance(100.0, up = false), 0.0)
        assertEquals(1900.0, stepDistance(2000.0, up = false), 0.0)
    }

    @Test
    fun `une distance hors grille est recalee`() {
        assertEquals(2200.0, stepDistance(2137.0, up = true), 0.0)
        assertEquals(2100.0, stepDistance(2137.0, up = false), 0.0)
    }

    @Test
    fun `le demi pas et le zero`() {
        assertEquals(32.5, stepDistance(30.0, up = true, fraction = 0.5), 0.0)
        assertEquals(0.0, stepDistance(5.0, up = false), 0.0)
        assertEquals(0.0, stepDistance(0.0, up = false), 0.0)
        assertEquals(5.0, stepDistance(null, up = true), 0.0)
    }
}

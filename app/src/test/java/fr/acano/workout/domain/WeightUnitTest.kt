package fr.acano.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUnitTest {

    @Test
    fun `une livre vaut 0,45359237 kilo`() {
        assertEquals(100.0, WeightUnit.LB.fromKg(45.359237), 1e-9)
        assertEquals(45.359237, WeightUnit.LB.toKg(100.0), 1e-9)
        assertEquals(42.5, WeightUnit.KG.fromKg(42.5), 0.0)
    }

    @Test
    fun `en livres les pas de charge sont arrondis a une valeur ronde`() {
        assertEquals(1.0, WeightUnit.LB.step(0.5), 0.0)
        assertEquals(2.5, WeightUnit.LB.step(1.0), 0.0)
        assertEquals(2.5, WeightUnit.LB.step(1.25), 0.0)
        assertEquals(5.0, WeightUnit.LB.step(2.5), 0.0)
        assertEquals(10.0, WeightUnit.LB.step(5.0), 0.0)
        assertEquals(20.0, WeightUnit.LB.step(10.0), 0.0)
    }

    @Test
    fun `en kilos le pas reste celui de l exercice`() {
        assertEquals(2.5, WeightUnit.KG.step(2.5), 0.0)
        assertEquals(1.25, WeightUnit.KG.step(1.25), 0.0)
    }

    @Test
    fun `en kilos une charge sur la grille avance comme avant`() {
        assertEquals(45.0, stepWeight(42.5, 2.5, WeightUnit.KG), 1e-9)
        assertEquals(40.0, stepWeight(42.5, -2.5, WeightUnit.KG), 1e-9)
        assertEquals(43.75, stepWeight(42.5, 1.25, WeightUnit.KG), 1e-9)
    }

    @Test
    fun `en livres le pas se cale sur la grille`() {
        // 45 kg = 99,2 lb : +5 lb mène à 100 lb, −5 lb à 95 lb.
        assertEquals(100.0, WeightUnit.LB.fromKg(stepWeight(45.0, 5.0, WeightUnit.LB)), 1e-9)
        assertEquals(95.0, WeightUnit.LB.fromKg(stepWeight(45.0, -5.0, WeightUnit.LB)), 1e-9)
    }

    @Test
    fun `une charge deja sur la grille en livres y reste malgre l aller retour`() {
        val hundredLb = WeightUnit.LB.toKg(100.0)
        assertEquals(105.0, WeightUnit.LB.fromKg(stepWeight(hundredLb, 5.0, WeightUnit.LB)), 1e-9)
        assertEquals(95.0, WeightUnit.LB.fromKg(stepWeight(hundredLb, -5.0, WeightUnit.LB)), 1e-9)
    }

    @Test
    fun `une charge absente part de zero et ne devient jamais negative`() {
        assertEquals(5.0, WeightUnit.LB.fromKg(stepWeight(null, 5.0, WeightUnit.LB)), 1e-9)
        assertEquals(0.0, stepWeight(1.0, -2.5, WeightUnit.KG), 0.0)
    }
}

package fr.acano.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarTest {

    @Test
    fun `chaque semaine compte sept cases`() {
        (1..12).forEach { m ->
            assertTrue(monthGrid(YearMonth.of(2026, m)).all { it.size == 7 })
        }
    }

    @Test
    fun `un mois commencant un mardi laisse le lundi vide`() {
        // 1er septembre 2026 : un mardi.
        val first = monthGrid(YearMonth.of(2026, 9)).first()
        assertNull(first[0])
        assertEquals(LocalDate.of(2026, 9, 1), first[1])
    }

    @Test
    fun `un mois commencant un dimanche place le 1er en derniere colonne`() {
        // 1er février 2026 : un dimanche.
        val first = monthGrid(YearMonth.of(2026, 2)).first()
        assertEquals(List(6) { null }, first.take(6))
        assertEquals(LocalDate.of(2026, 2, 1), first[6])
    }

    @Test
    fun `tous les jours du mois apparaissent une fois et dans l ordre`() {
        val days = monthGrid(YearMonth.of(2026, 9)).flatten().filterNotNull()
        assertEquals((1..30).map { LocalDate.of(2026, 9, it) }, days)
    }

    @Test
    fun `seules les semaines utiles sont produites`() {
        // Février 2027 commence un lundi et compte 28 jours : quatre semaines pile.
        assertEquals(4, monthGrid(YearMonth.of(2027, 2)).size)
        // Août 2026 commence un samedi : six semaines.
        assertEquals(6, monthGrid(YearMonth.of(2026, 8)).size)
    }
}

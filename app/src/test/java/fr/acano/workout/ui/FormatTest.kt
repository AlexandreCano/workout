package fr.acano.workout.ui

import fr.acano.workout.timer.formatClock
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.formatWeightValue
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `les poids decimaux s affichent avec une virgule`() {
        assertEquals("42,5 kg", formatWeight(42.5))
        assertEquals("1,25 kg", formatWeight(1.25))
    }

    @Test
    fun `les poids entiers n affichent pas de decimale`() {
        assertEquals("45 kg", formatWeight(45.0))
        assertEquals("100 kg", formatWeight(100.0))
    }

    @Test
    fun `un poids inconnu s affiche comme un tiret`() {
        assertEquals("—", formatWeight(null))
    }

    @Test
    fun `la valeur brute de poids ne porte pas d unite`() {
        assertEquals("42,5", formatWeightValue(42.5))
        assertEquals("45", formatWeightValue(45.0))
    }

    @Test
    fun `le chrono s affiche en minutes et secondes`() {
        assertEquals("01:00", formatClock(60_000))
        assertEquals("00:47", formatClock(46_500))
        assertEquals("05:00", formatClock(300_000))
        assertEquals("00:00", formatClock(0))
    }

    @Test
    fun `le chrono ne descend jamais sous zero`() {
        assertEquals("00:00", formatClock(-5_000))
    }

    @Test
    fun `la duree d une seance s affiche en minutes puis en heures`() {
        assertEquals("47 min", formatDuration(47 * 60_000L))
        assertEquals("1 h 12", formatDuration(72 * 60_000L))
    }
}

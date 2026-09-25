package fr.acano.workout.ui

import fr.acano.workout.domain.WeightUnit
import fr.acano.workout.timer.formatClock
import fr.acano.workout.ui.common.formatDate
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatLongDate
import fr.acano.workout.ui.common.formatSet
import fr.acano.workout.ui.common.formatVolume
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.formatWeightValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

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
    fun `en livres le poids est converti et arrondi au dixieme`() {
        assertEquals("100 lb", formatWeight(WeightUnit.LB.toKg(100.0), WeightUnit.LB))
        assertEquals("99,2 lb", formatWeight(45.0, WeightUnit.LB))
        assertEquals("—", formatWeight(null, WeightUnit.LB))
    }

    @Test
    fun `une serie chargee affiche sa charge et ses repetitions`() {
        assertEquals("42,5 kg × 12", formatSet(42.5, 12, null))
        assertEquals("100 lb × 8", formatSet(WeightUnit.LB.toKg(100.0), 8, null, WeightUnit.LB))
    }

    @Test
    fun `une serie sans charge ou chronometree s affiche selon ce qui a ete fait`() {
        assertEquals("12 reps", formatSet(null, 12, null))
        assertEquals("1 rep", formatSet(null, 1, null))
        assertEquals("45 s", formatSet(null, null, 45))
        assertEquals("1 min", formatSet(null, null, 60))
        assertEquals("1 min 30", formatSet(null, null, 90))
        assertEquals("—", formatSet(null, null, null))
    }

    @Test
    fun `la charge totale est arrondie et groupee par milliers`() {
        // Le séparateur de milliers français est une espace fine insécable (U+202F) :
        // on compare sans les espaces, quelles qu'elles soient.
        assertEquals("3815kg", formatVolume(3815.4).filter { it.isLetterOrDigit() })
        assertEquals("3", formatVolume(3815.4).substringBefore('8').trim())
        assertEquals("480 kg", formatVolume(480.0))
    }

    private fun at(day: LocalDate) = day.atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val today = LocalDate.of(2026, 9, 25)

    @Test
    fun `les dates recentes sont relatives puis courtes sans l annee en cours`() {
        assertEquals("aujourd'hui", formatDate(at(today), today, ZoneOffset.UTC))
        assertEquals("hier", formatDate(at(today.minusDays(1)), today, ZoneOffset.UTC))
        assertEquals("il y a 3 jours", formatDate(at(today.minusDays(3)), today, ZoneOffset.UTC))
        assertEquals("il y a 6 jours", formatDate(at(today.minusDays(6)), today, ZoneOffset.UTC))
        assertEquals("18 sept.", formatDate(at(today.minusDays(7)), today, ZoneOffset.UTC))
        assertEquals("25 sept. 2025", formatDate(at(today.minusYears(1)), today, ZoneOffset.UTC))
    }

    @Test
    fun `la date longue n affiche l annee que si elle differe`() {
        assertEquals("Mardi 22 septembre", formatLongDate(at(LocalDate.of(2026, 9, 22)), today, ZoneOffset.UTC))
        assertEquals("Lundi 22 septembre 2025", formatLongDate(at(LocalDate.of(2025, 9, 22)), today, ZoneOffset.UTC))
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

package fr.acano.workout.ui

import fr.acano.workout.domain.WeightUnit
import fr.acano.workout.timer.formatClock
import fr.acano.workout.ui.common.formatDuration
import fr.acano.workout.ui.common.formatSet
import fr.acano.workout.ui.common.formatVolume
import fr.acano.workout.ui.common.formatWeight
import fr.acano.workout.ui.common.formatWeightValue
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Les nombres suivent la langue du téléphone : ces tests fixent le français, sauf mention contraire. */
class FormatTest {

    private val saved = Locale.getDefault()

    @Before
    fun french() = Locale.setDefault(Locale.FRENCH)

    @After
    fun restore() = Locale.setDefault(saved)

    @Test
    fun `en anglais le separateur decimal est un point`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("42.5 kg", formatWeight(42.5))
        assertEquals("3,815 kg", formatVolume(3815.4))
    }

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

package fr.acano.workout.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.ui.common.formatDate
import fr.acano.workout.ui.common.formatLongDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Les dates dans les deux langues de l'application, avec les vraies ressources. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizedFormatTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 9, 25)
    private fun at(day: LocalDate) = day.atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun date(day: LocalDate) = formatDate(context, at(day), today, ZoneOffset.UTC)
    private fun long(day: LocalDate) = formatLongDate(context, at(day), today, ZoneOffset.UTC)

    @Test
    @Config(qualifiers = "fr")
    fun `en francais les dates recentes sont relatives puis courtes`() {
        assertEquals("aujourd'hui", date(today))
        assertEquals("hier", date(today.minusDays(1)))
        assertEquals("il y a 3 jours", date(today.minusDays(3)))
        assertEquals("18 sept.", date(today.minusDays(7)))
        assertEquals("25 sept. 2025", date(today.minusYears(1)))
        assertEquals("Mardi 22 septembre", long(LocalDate.of(2026, 9, 22)))
        assertEquals("Lundi 22 septembre 2025", long(LocalDate.of(2025, 9, 22)))
    }

    @Test
    @Config(qualifiers = "en")
    fun `en anglais le mois precede le jour`() {
        assertEquals("today", date(today))
        assertEquals("yesterday", date(today.minusDays(1)))
        assertEquals("3 days ago", date(today.minusDays(3)))
        assertEquals("Sep 18", date(today.minusDays(7)))
        assertEquals("Sep 25, 2025", date(today.minusYears(1)))
        assertEquals("Tuesday, September 22", long(LocalDate.of(2026, 9, 22)))
    }

    @Test
    @Config(qualifiers = "de")
    fun `une langue non prise en charge retombe sur l anglais`() {
        assertEquals("yesterday", date(today.minusDays(1)))
    }
}

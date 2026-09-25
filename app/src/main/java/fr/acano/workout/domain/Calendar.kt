package fr.acano.workout.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Les cases d'un mois, semaine par semaine, en commençant par [firstDayOfWeek].
 * Les cases hors du mois valent `null` : la grille garde ainsi toujours 7 colonnes
 * alignées sur les jours de la semaine, et seules les semaines utiles sont produites.
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<List<LocalDate?>> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = cells + List((7 - cells.size % 7) % 7) { null }
    return padded.chunked(7)
}

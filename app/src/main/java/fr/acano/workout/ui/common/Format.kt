package fr.acano.workout.ui.common

import fr.acano.workout.data.db.entity.ExerciseEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)
private val dayWithYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
private val shortDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

fun formatDay(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dayFormatter)

fun formatDayWithYear(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dayWithYearFormatter)

fun formatShortDay(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(shortDayFormatter)

/** « 47 min », ou « 1 h 12 » au-delà d'une heure. */
fun formatDuration(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${"%02d".format(minutes % 60)}"
}

/** « 42,5 kg » — virgule décimale, et pas de « ,0 » inutile. */
fun formatWeight(kg: Double?): String {
    if (kg == null) return "—"
    val text = if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.2f".format(Locale.FRENCH, kg).trimEnd('0').trimEnd(',')
    return "$text kg".replace('.', ',')
}

fun formatWeightValue(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.2f".format(Locale.FRENCH, kg).trimEnd('0').trimEnd(',')

/** « 8–12 reps », « 1 min », « 3–5 répétitions » selon le type d'exercice. */
fun ExerciseEntity.targetLabel(): String = when {
    targetDurationSeconds != null -> {
        val seconds = targetDurationSeconds
        if (seconds % 60 == 0) "${seconds / 60} min" else "$seconds s"
    }
    targetRepsMin != null && targetRepsMax != null ->
        if (targetRepsMin == targetRepsMax) "$targetRepsMin reps" else "$targetRepsMin–$targetRepsMax reps"
    else -> ""
}

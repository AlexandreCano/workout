package fr.acano.workout.ui.common

import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.PlannedStep
import fr.acano.workout.domain.WeightUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale





/** « 47 min », ou « 1 h 12 » au-delà d'une heure. */
fun formatDuration(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${"%02d".format(minutes % 60)}"
}

/**
 * « 42,5 kg » ou « 93,7 lb » — virgule décimale, et pas de « ,0 » inutile.
 * [kg] est toujours en kilogrammes ; la conversion se fait ici, à l'affichage.
 */
fun formatWeight(kg: Double?, unit: WeightUnit = WeightUnit.KG): String {
    if (kg == null) return "—"
    return "${formatWeightValue(kg, unit)} ${unit.symbol}"
}

/** La valeur seule, sans unité. */
fun formatWeightValue(kg: Double, unit: WeightUnit = WeightUnit.KG): String =
    formatNumber(unit.fromKg(kg), decimals = if (unit == WeightUnit.KG) 2 else 1)

/** Un nombre déjà dans l'unité voulue (un pas de charge en livres, par exemple). */
fun formatNumber(value: Double, decimals: Int = 2): String {
    // Arrondi d'abord : 45 kg = 99,2080… lb, et 100 lb reconverti depuis les kg
    // doit redonner « 100 », pas « 99,99 ».
    val text = "%.${decimals}f".format(Locale.FRENCH, value)
    return if (text.contains(',')) text.trimEnd('0').trimEnd(',') else text
}

/** « 8–12 reps » ou « 1 min » : la cible d'une étape, selon ce qui la définit. */
fun targetLabel(repsMin: Int?, repsMax: Int?, durationSeconds: Int?): String = when {
    durationSeconds != null ->
        if (durationSeconds % 60 == 0) "${durationSeconds / 60} min" else "$durationSeconds s"
    repsMin != null && repsMax != null ->
        if (repsMin == repsMax) "$repsMin reps" else "$repsMin–$repsMax reps"
    else -> ""
}

fun PlannedStep.targetLabel(): String = targetLabel(targetRepsMin, targetRepsMax, targetDurationSeconds)

/** Ce que l'exercice demande, pour le catalogue : il n'a plus de séries ni de reps à lui. */
val ExerciseKind.label: String
    get() = when (this) {
        ExerciseKind.WEIGHTED_REPS -> "Charge et répétitions"
        ExerciseKind.REPS_ONLY -> "Répétitions"
        ExerciseKind.TIMED -> "Chronométré"
    }

/**
 * Une série telle qu'elle a été faite : « 42,5 kg × 12 », « 12 reps » ou
 * « 1 min 30 » selon ce qui a été enregistré.
 */
fun formatSet(weightKg: Double?, repetitions: Int?, durationSeconds: Int?, unit: WeightUnit = WeightUnit.KG): String =
    when {
        weightKg != null && repetitions != null -> "${formatWeight(weightKg, unit)} × $repetitions"
        repetitions != null -> if (repetitions > 1) "$repetitions reps" else "1 rep"
        durationSeconds != null -> {
            val minutes = durationSeconds / 60
            val rest = durationSeconds % 60
            when {
                minutes == 0 -> "$rest s"
                rest == 0 -> "$minutes min"
                else -> "$minutes min %02d".format(rest)
            }
        }
        weightKg != null -> formatWeight(weightKg, unit)
        else -> "—"
    }

/**
 * Charge totale soulevée, arrondie et groupée par milliers : « 1 230 kg ».
 * Au-delà de la centaine, les décimales n'apportent rien.
 */
fun formatVolume(kg: Double, unit: WeightUnit = WeightUnit.KG): String =
    "${java.text.NumberFormat.getIntegerInstance(Locale.FRENCH).format(kotlin.math.round(unit.fromKg(kg)).toLong())} ${unit.symbol}"

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private val shortDateWithYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)
private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val longDateWithYearFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)

private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * La date d'un évènement passé, dans le format unique de l'application :
 * « aujourd'hui », « hier », « il y a 3 jours » pour la semaine écoulée, puis
 * « 22 sept. » — l'année n'apparaît que si ce n'est pas l'année en cours.
 */
fun formatDate(
    epochMillis: Long,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val day = epochMillis.toLocalDate(zone)
    val days = ChronoUnit.DAYS.between(day, today)
    return when {
        days == 0L -> "aujourd'hui"
        days == 1L -> "hier"
        days in 2..6 -> "il y a $days jours"
        day.year == today.year -> day.format(shortDateFormatter)
        else -> day.format(shortDateWithYearFormatter)
    }
}

/** En-tête d'une séance : « mardi 22 septembre », avec l'année si elle diffère. */
fun formatLongDate(
    epochMillis: Long,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val day = epochMillis.toLocalDate(zone)
    val text = day.format(if (day.year == today.year) longDateFormatter else longDateWithYearFormatter)
    return text.replaceFirstChar { it.titlecase(Locale.FRENCH) }
}

/** « ≈ 245 kcal » : arrondi à l'unité, et le « ≈ » rappelle qu'il s'agit d'une estimation. */
fun formatKcal(kcal: Double): String = "≈ ${kotlin.math.round(kcal).toInt()} kcal"

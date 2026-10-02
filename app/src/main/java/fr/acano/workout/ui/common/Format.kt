package fr.acano.workout.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import fr.acano.workout.R
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.PlannedStep
import fr.acano.workout.domain.WeightUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * Formats d'affichage. Les nombres et les dates suivent la langue du téléphone
 * (« 42,5 kg » en français, « 42.5 kg » en anglais) ; les unités — kg, lb,
 * min, s, reps, kcal — s'écrivent de la même façon dans les deux langues.
 */

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

/**
 * Un nombre déjà dans l'unité voulue (un pas de charge en livres, par exemple),
 * sans zéros inutiles et avec le séparateur décimal de la langue.
 */
fun formatNumber(value: Double, decimals: Int = 2, locale: Locale = Locale.getDefault()): String {
    // Arrondi d'abord : 45 kg = 99,2080… lb, et 100 lb reconverti depuis les kg
    // doit redonner « 100 », pas « 99,99 ».
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
        isGroupingUsed = false
    }
    return format.format(value)
}

/** « 8–12 reps », « 1 min » ou « 30 m » : la cible d'une étape, selon ce qui la définit. */
fun targetLabel(repsMin: Int?, repsMax: Int?, durationSeconds: Int?, distanceMeters: Int? = null): String = when {
    durationSeconds != null ->
        if (durationSeconds % 60 == 0) "${durationSeconds / 60} min" else "$durationSeconds s"
    distanceMeters != null -> formatDistance(distanceMeters.toDouble())
    repsMin != null && repsMax != null ->
        if (repsMin == repsMax) "$repsMin reps" else "$repsMin–$repsMax reps"
    else -> ""
}

fun PlannedStep.targetLabel(): String =
    targetLabel(targetRepsMin, targetRepsMax, targetDurationSeconds, targetDistanceMeters)

/**
 * « 850 m », ou « 2,15 km » à partir du kilomètre. Les distances sont
 * toujours en mètres, quelle que soit l'unité de charge choisie : les
 * machines de cardio des salles affichent des kilomètres.
 */
fun formatDistance(meters: Double, locale: Locale = Locale.getDefault()): String =
    if (meters < 1000) {
        "${formatNumber(meters, decimals = 0, locale = locale)} m"
    } else {
        "${formatNumber(meters / 1000, decimals = 2, locale = locale)} km"
    }

/** Ce que l'exercice demande, pour le catalogue : il n'a plus de séries ni de reps à lui. */
val ExerciseKind.label: String
    @Composable get() = stringResource(labelRes)

val ExerciseKind.labelRes: Int
    get() = when (this) {
        ExerciseKind.WEIGHTED_REPS -> R.string.kind_weighted_reps
        ExerciseKind.REPS_ONLY -> R.string.kind_reps_only
        ExerciseKind.TIMED -> R.string.kind_timed
        ExerciseKind.WEIGHTED_TIMED -> R.string.kind_weighted_timed
        ExerciseKind.DISTANCE -> R.string.kind_distance
        ExerciseKind.WEIGHTED_DISTANCE -> R.string.kind_weighted_distance
        ExerciseKind.TIMED_DISTANCE -> R.string.kind_timed_distance
    }

/**
 * Une série telle qu'elle a été faite : « 42,5 kg × 12 », « 12 reps »,
 * « 1 min 30 » ou « 10 min · 2,1 km » selon ce qui a été enregistré.
 */
fun formatSet(
    weightKg: Double?,
    repetitions: Int?,
    durationSeconds: Int?,
    unit: WeightUnit = WeightUnit.KG,
    distanceMeters: Double? = null,
): String =
    if (weightKg != null && repetitions != null) {
        "${formatWeight(weightKg, unit)} × $repetitions"
    } else {
        listOfNotNull(
            weightKg?.let { formatWeight(it, unit) },
            repetitions?.let { if (it > 1) "$it reps" else "1 rep" },
            durationSeconds?.let(::formatSeconds),
            distanceMeters?.let { formatDistance(it) },
        ).joinToString(" · ").ifEmpty { "—" }
    }

/** « 45 s », « 1 min 30 », « 5 min ». */
private fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes == 0 -> "$rest s"
        rest == 0 -> "$minutes min"
        else -> "$minutes min %02d".format(rest)
    }
}

/**
 * Charge totale soulevée, arrondie et groupée par milliers : « 1 230 kg ».
 * Au-delà de la centaine, les décimales n'apportent rien.
 */
fun formatVolume(kg: Double, unit: WeightUnit = WeightUnit.KG, locale: Locale = Locale.getDefault()): String =
    "${NumberFormat.getIntegerInstance(locale).format(kotlin.math.round(unit.fromKg(kg)).toLong())} ${unit.symbol}"

private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * La date d'un évènement passé, dans le format unique de l'application :
 * « aujourd'hui », « hier », « il y a 3 jours » pour la semaine écoulée, puis
 * « 22 sept. » (« Sep 22 » en anglais) — l'année n'apparaît que si ce n'est pas
 * l'année en cours.
 */
fun formatDate(
    context: Context,
    epochMillis: Long,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val res = context.resources
    val day = epochMillis.toLocalDate(zone)
    val days = ChronoUnit.DAYS.between(day, today)
    return when {
        days == 0L -> res.getString(R.string.date_today)
        days == 1L -> res.getString(R.string.date_yesterday)
        days in 2..6 -> res.getQuantityString(R.plurals.date_days_ago, days.toInt(), days.toInt())
        else -> day.format(
            pattern(context, if (day.year == today.year) R.string.date_pattern_short else R.string.date_pattern_short_year),
        )
    }
}

@Composable
fun formatDate(epochMillis: Long): String {
    LocalConfiguration.current // se recalcule quand la langue change
    return formatDate(LocalContext.current, epochMillis)
}

/** En-tête d'une séance : « Mardi 22 septembre » / « Tuesday, September 22 », avec l'année si elle diffère. */
fun formatLongDate(
    context: Context,
    epochMillis: Long,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val day = epochMillis.toLocalDate(zone)
    val text = day.format(
        pattern(context, if (day.year == today.year) R.string.date_pattern_long else R.string.date_pattern_long_year),
    )
    return text.replaceFirstChar { it.titlecase(context.locale) }
}

@Composable
fun formatLongDate(epochMillis: Long): String {
    LocalConfiguration.current
    return formatLongDate(LocalContext.current, epochMillis)
}

/** Motif de date lu dans les ressources, appliqué dans la langue de l'interface. */
fun pattern(context: Context, patternRes: Int): DateTimeFormatter =
    DateTimeFormatter.ofPattern(context.getString(patternRes), context.locale)

val Context.locale: Locale get() = resources.configuration.locales[0]

/** « ≈ 245 kcal » : arrondi à l'unité, et le « ≈ » rappelle qu'il s'agit d'une estimation. */
fun formatKcal(kcal: Double): String = "≈ ${kotlin.math.round(kcal).toInt()} kcal"

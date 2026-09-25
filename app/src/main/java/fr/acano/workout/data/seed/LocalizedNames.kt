package fr.acano.workout.data.seed

import android.content.Context
import android.content.res.Configuration
import fr.acano.workout.R
import fr.acano.workout.domain.WorkoutType
import java.util.Locale

/**
 * Les noms du contenu fourni par l'application, dans la langue du téléphone.
 *
 * Ils sont stockés en base (les séries y font référence, l'historique les
 * affiche) : on les y réécrit au démarrage et à chaque changement de langue.
 * Pour les deux entraînements du programme, qu'on peut renommer, seuls ceux qui
 * portent encore un nom d'origine — dans l'une des langues de l'app — changent.
 */
data class LocalizedNames(
    val exercises: Map<String, String>,
    val programs: Map<WorkoutType, String>,
    /** Tous les noms d'origine connus d'un programme, toutes langues confondues. */
    val knownProgramNames: Map<WorkoutType, List<String>>,
) {
    companion object {
        private val exerciseRes = mapOf(
            Program.BIKE to R.string.exercise_bike_warmup,
            "chest_press" to R.string.exercise_chest_press,
            "pec_deck" to R.string.exercise_pec_deck,
            "lat_pulldown" to R.string.exercise_lat_pulldown,
            "seated_row" to R.string.exercise_seated_row,
            "leg_press" to R.string.exercise_leg_press,
            "leg_curl" to R.string.exercise_leg_curl,
            "leg_extension" to R.string.exercise_leg_extension,
            "calf_raise" to R.string.exercise_calf_raise,
            Program.PLANK to R.string.exercise_plank,
            Program.STOMACH_VACUUM to R.string.exercise_stomach_vacuum,
        )
        private val programRes = mapOf(
            WorkoutType.UPPER_BODY to R.string.program_upper_body,
            WorkoutType.LOWER_BODY to R.string.program_lower_body,
        )
        private val supportedLocales = listOf(Locale.ENGLISH, Locale.FRENCH)

        fun from(context: Context): LocalizedNames {
            val others = supportedLocales.map { locale ->
                context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
            }
            return LocalizedNames(
                exercises = exerciseRes.mapValues { context.getString(it.value) },
                programs = programRes.mapValues { context.getString(it.value) },
                knownProgramNames = programRes.mapValues { (type, res) ->
                    (others.map { it.getString(res) } + type.label).distinct()
                },
            )
        }
    }
}

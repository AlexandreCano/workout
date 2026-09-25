package fr.acano.workout.domain

import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.seed.Program

/** Sexe utilisé par la formule de métabolisme de base (Mifflin-St Jeor). */
enum class Sex { MALE, FEMALE }

/** Ce que l'utilisateur renseigne dans les réglages pour estimer ses calories. */
data class UserProfile(
    val sex: Sex? = null,
    val weightKg: Double? = null,
    val heightCm: Int? = null,
    /** Facultative : sans elle, le calcul se contente du poids. */
    val birthYear: Int? = null,
) {
    /** Le poids suffit à une estimation ; le reste l'affine. */
    val canEstimate: Boolean get() = weightKg != null && weightKg > 0
}

/**
 * Estimation des calories dépensées, par la méthode du Compendium des
 * activités physiques : chaque activité a un MET (son coût énergétique
 * rapporté au repos), et la dépense vaut MET × poids × durée.
 *
 * Le MET standard suppose un métabolisme de repos de 3,5 ml d'O₂/kg/min.
 * Quand le profil est complet (sexe, taille, âge), on calcule le vrai
 * métabolisme de repos par Mifflin-St Jeor et on corrige le MET en
 * conséquence — c'est la correction recommandée par le Compendium.
 *
 * Seuls les exercices de l'application ont un MET connu ; ceux créés par
 * l'utilisateur ne sont pas estimés.
 */
object Calories {

    /** MET du Compendium (2011, codes entre parenthèses) pour les exercices fournis. */
    private val metByExercise: Map<String, Double> = mapOf(
        // Vélo stationnaire, effort léger d'échauffement (02011 : 51–89 W).
        Program.BIKE to 5.5,
        // Musculation sur machines, 8–15 répétitions, charge modérée (02054).
        "chest_press" to 3.5,
        "pec_deck" to 3.5,
        "lat_pulldown" to 3.5,
        "seated_row" to 3.5,
        "leg_curl" to 3.5,
        "leg_extension" to 3.5,
        "calf_raise" to 3.5,
        // Mouvement polyarticulaire des jambes, plus exigeant (02052).
        "leg_press" to 5.0,
        // Gainage, effort modéré (02068).
        Program.PLANK to 3.8,
        // Respiration et contraction abdominale debout : à peine plus que le repos.
        Program.STOMACH_VACUUM to 1.5,
    )

    /** Au-delà, une pause oubliée gonflerait l'estimation plus qu'elle ne mesure l'effort. */
    private const val MAX_MINUTES_PER_EXERCISE = 30.0

    fun hasEstimate(exerciseId: String): Boolean = exerciseId in metByExercise

    /**
     * Kilocalories par minute pour un MET donné. Avec un profil complet, le
     * MET est corrigé par le métabolisme de repos réel ; sinon on applique la
     * formule standard MET × 3,5 × poids / 200.
     */
    fun kcalPerMinute(met: Double, profile: UserProfile, currentYear: Int): Double? {
        val weight = profile.weightKg?.takeIf { it > 0 } ?: return null
        val restingMlPerKgMin = restingMetabolism(profile, currentYear) ?: STANDARD_RESTING
        val correctedMet = met * STANDARD_RESTING / restingMlPerKgMin
        return correctedMet * STANDARD_RESTING * weight / 200.0
    }

    /** Métabolisme de repos en ml d'O₂/kg/min, ou null si le profil ne permet pas de le calculer. */
    internal fun restingMetabolism(profile: UserProfile, currentYear: Int): Double? {
        val weight = profile.weightKg ?: return null
        val height = profile.heightCm ?: return null
        val sex = profile.sex ?: return null
        val age = currentYear - (profile.birthYear ?: return null)
        if (age !in 10..110) return null
        // Mifflin-St Jeor, en kcal par jour.
        val kcalPerDay = 10 * weight + 6.25 * height - 5 * age + if (sex == Sex.MALE) 5 else -161
        // 1 litre d'O₂ ≈ 5 kcal ; ramené à la minute et au kilo.
        return kcalPerDay * 1000 / (1440 * 5 * weight)
    }

    /**
     * Durée réelle de chaque exercice d'une séance, en minutes, repos compris :
     * du moment où l'exercice précédent s'est terminé jusqu'à la dernière série
     * de celui-ci. Les exercices sont pris dans l'ordre où ils ont réellement
     * été faits, pas dans l'ordre prévu.
     *
     * [steps] : les séries de chaque étape ; le résultat a le même ordre.
     */
    fun exerciseMinutes(sessionStartedAt: Long, steps: List<List<SetResultEntity>>): List<Double> {
        val minutes = MutableList(steps.size) { 0.0 }
        var previousEnd = sessionStartedAt
        steps.withIndex()
            .filter { it.value.isNotEmpty() }
            .sortedBy { step -> step.value.minOf { it.completedAt } }
            .forEach { (index, sets) ->
                val end = sets.maxOf { it.completedAt }
                val elapsed = (end - previousEnd).coerceAtLeast(0) / 60_000.0
                // Une série chronométrée dure au moins ce qu'elle a duré.
                val timed = sets.sumOf { it.durationSeconds ?: 0 } / 60.0
                minutes[index] = maxOf(elapsed, timed).coerceAtMost(MAX_MINUTES_PER_EXERCISE)
                previousEnd = end
            }
        return minutes
    }

    /**
     * Calories de chaque étape d'une séance, dans l'ordre de [steps] ; null pour
     * une étape sans estimation (exercice de l'utilisateur, profil incomplet).
     */
    fun perExercise(
        sessionStartedAt: Long,
        steps: List<Pair<String, List<SetResultEntity>>>,
        profile: UserProfile,
        currentYear: Int,
    ): List<Double?> {
        val minutes = exerciseMinutes(sessionStartedAt, steps.map { it.second })
        return steps.mapIndexed { index, (exerciseId, sets) ->
            val met = metByExercise[exerciseId] ?: return@mapIndexed null
            if (sets.isEmpty()) return@mapIndexed null
            kcalPerMinute(met, profile, currentYear)?.let { it * minutes[index] }
        }
    }

    private const val STANDARD_RESTING = 3.5
}

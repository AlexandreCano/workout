package fr.acano.workout.domain

/**
 * Type enregistré sur une séance. Depuis la v4 de la base, toute séance vient
 * d'un entraînement et vaut [CUSTOM] ; [UPPER_BODY] et [LOWER_BODY] ne subsistent
 * que pour les séances plus anciennes et pour décrire le programme d'origine
 * inséré au premier lancement (voir `ProgramSeed`).
 */
enum class WorkoutType(val label: String) {
    UPPER_BODY("Haut du corps"),
    LOWER_BODY("Bas du corps"),
    CUSTOM("Personnalisé"),
}

/**
 * Une étape d'entraînement : l'exercice, ses séries, sa cible et son repos.
 * La cible est une fourchette de répétitions ou une durée selon le type
 * d'exercice ; l'autre reste nulle.
 */
data class PlannedStep(
    val exerciseId: String,
    val plannedSets: Int,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSeconds: Int? = null,
    /** Repos après une série, en secondes. 0 = pas de chrono. */
    val restSeconds: Int = 60,
)

/**
 * Détermine l'interface de saisie d'un exercice.
 *
 * - [WEIGHTED_REPS] : poids + répétitions (machines).
 * - [TIMED] : durée fixe, déclenchée par un timer (vélo, planche).
 * - [REPS_ONLY] : quelques répétitions, sans charge (stomach vacuum).
 */
enum class ExerciseKind {
    WEIGHTED_REPS,
    TIMED,
    REPS_ONLY,
}

/**
 * Unité d'affichage et de saisie des charges. Les poids sont toujours stockés
 * en kilogrammes : changer d'unité ne réécrit donc aucune donnée, et
 * l'historique reste comparable d'une préférence à l'autre.
 */
enum class WeightUnit(val symbol: String, val spokenName: String) {
    KG("kg", "kilos"),
    LB("lb", "livres");

    fun fromKg(kg: Double): Double = if (this == KG) kg else kg / KG_PER_LB

    fun toKg(value: Double): Double = if (this == KG) value else value * KG_PER_LB

    /**
     * Pas de charge exprimé dans l'unité. Une conversion brute donnerait des pas
     * absurdes (2,5 kg ≈ 5,51 lb) : on retient le pas « rond » le plus proche,
     * tel qu'on le trouve sur les machines graduées en livres.
     */
    fun step(stepKg: Double): Double =
        if (this == KG) stepKg else LB_STEPS.minBy { kotlin.math.abs(it - fromKg(stepKg)) }

    companion object {
        const val KG_PER_LB = 0.45359237
        private val LB_STEPS = listOf(1.0, 2.5, 5.0, 10.0, 20.0)
    }
}

/**
 * Applique un pas de [delta] (exprimé dans [unit]) à une charge [currentKg] et
 * renvoie la nouvelle charge en kilogrammes, jamais négative.
 *
 * Le résultat est calé sur la grille du pas : en livres, 45 kg (99,2 lb) + 5 lb
 * donne 100 lb et non 104,2 lb, qu'aucune machine ne propose. En kilos, une
 * charge déjà sur la grille avance exactement comme avant.
 */
fun stepWeight(currentKg: Double?, delta: Double, unit: WeightUnit): Double {
    val grid = kotlin.math.abs(delta)
    if (grid == 0.0) return currentKg ?: 0.0
    val current = unit.fromKg(currentKg ?: 0.0)
    // La tolérance absorbe l'aller-retour kg ↔ lb : 100 lb relu depuis la base
    // vaut 99,999999… et doit rester « sur la grille ».
    val slot = current / grid
    val next = if (delta > 0) {
        (kotlin.math.floor(slot + GRID_TOLERANCE) + 1) * grid
    } else {
        (kotlin.math.ceil(slot - GRID_TOLERANCE) - 1) * grid
    }
    return unit.toKg(next.coerceAtLeast(0.0))
}

private const val GRID_TOLERANCE = 1e-6

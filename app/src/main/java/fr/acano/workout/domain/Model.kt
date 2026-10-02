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
    /** Distance visée, en mètres, pour un exercice qui se mesure en distance sans chrono. */
    val targetDistanceMeters: Int? = null,
)

/**
 * Détermine l'interface de saisie d'un exercice : ce qu'on note à chaque série.
 *
 * - [WEIGHTED_REPS] : poids + répétitions (machines).
 * - [TIMED] : durée fixe, déclenchée par un timer (vélo, planche).
 * - [REPS_ONLY] : des répétitions, sans charge (pompes, stomach vacuum).
 * - [WEIGHTED_TIMED] : une durée sous charge (planche lestée).
 * - [DISTANCE] : une distance, sans charge ni chrono.
 * - [WEIGHTED_DISTANCE] : une charge portée sur une distance (farmer carry, traîneau).
 * - [TIMED_DISTANCE] : une durée chronométrée, puis la distance parcourue (tapis, rameur).
 *
 * Les noms sont stockés en base : on en ajoute, on n'en renomme pas.
 */
enum class ExerciseKind(
    val hasWeight: Boolean = false,
    val hasReps: Boolean = false,
    /** La série se fait au chrono, sa cible est une durée. */
    val isTimed: Boolean = false,
    val hasDistance: Boolean = false,
) {
    WEIGHTED_REPS(hasWeight = true, hasReps = true),
    TIMED(isTimed = true),
    REPS_ONLY(hasReps = true),
    WEIGHTED_TIMED(hasWeight = true, isTimed = true),
    DISTANCE(hasDistance = true),
    WEIGHTED_DISTANCE(hasWeight = true, hasDistance = true),
    TIMED_DISTANCE(isTimed = true, hasDistance = true),
    ;

    /** La cible est une distance : ni reps, ni chrono. */
    val targetsDistance: Boolean get() = hasDistance && !isTimed
}

/**
 * Unité d'affichage et de saisie des charges. Les poids sont toujours stockés
 * en kilogrammes : changer d'unité ne réécrit donc aucune donnée, et
 * l'historique reste comparable d'une préférence à l'autre.
 */
enum class WeightUnit(val symbol: String) {
    KG("kg"),
    LB("lb");

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

/**
 * Le pas d'une distance, en mètres, selon son ordre de grandeur : 5 m pour
 * un farmer carry, 50 m pour quelques centaines de mètres, 100 m au-delà du
 * kilomètre, 500 m au-delà de 10 km.
 */
fun distanceStep(meters: Double): Double = when {
    meters < 100 -> 5.0
    meters < 1000 -> 50.0
    meters < 10_000 -> 100.0
    else -> 500.0
}

/**
 * Applique un pas de distance vers le haut ([up]) ou vers le bas, calé sur la
 * grille du pas ; [fraction] vaut 0,5 pour un demi-pas. Jamais sous zéro.
 */
fun stepDistance(currentMeters: Double?, up: Boolean, fraction: Double = 1.0): Double {
    val current = currentMeters ?: 0.0
    // En descendant, le pas est celui de la zone qu'on quitte par le bas :
    // 1000 m − un pas donne 950 m, pas 900 m.
    val step = distanceStep(if (up) current else (current - 1e-6).coerceAtLeast(0.0)) * fraction
    val slot = current / step
    val next = if (up) {
        (kotlin.math.floor(slot + GRID_TOLERANCE) + 1) * step
    } else {
        (kotlin.math.ceil(slot - GRID_TOLERANCE) - 1) * step
    }
    return next.coerceAtLeast(0.0)
}

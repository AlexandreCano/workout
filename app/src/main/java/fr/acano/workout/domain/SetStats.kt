package fr.acano.workout.domain

import fr.acano.workout.data.db.entity.SetResultEntity

/**
 * Indice de la meilleure série d'un exercice dans une séance : la plus lourde,
 * départagée par le nombre de répétitions ; à défaut de charge, le plus de
 * répétitions ; pour un exercice chronométré, la plus longue.
 *
 * Null s'il n'y a qu'une série : la mettre en avant n'apprendrait rien.
 */
fun bestSetIndex(sets: List<SetResultEntity>): Int? {
    if (sets.size < 2) return null
    val best = sets.withIndex().maxWithOrNull(
        compareBy<IndexedValue<SetResultEntity>> { it.value.weightKg ?: 0.0 }
            .thenBy { it.value.repetitions ?: 0 }
            .thenBy { it.value.durationSeconds ?: 0 },
    ) ?: return null
    // Toutes les séries identiques : aucune ne se distingue.
    val first = sets.first()
    if (sets.all { it.weightKg == first.weightKg && it.repetitions == first.repetitions && it.durationSeconds == first.durationSeconds }) {
        return null
    }
    return best.index
}

/** Charge totale soulevée : somme des charges × répétitions, en kilos. */
fun volumeKg(sets: List<SetResultEntity>): Double =
    sets.sumOf { (it.weightKg ?: 0.0) * (it.repetitions ?: 0) }

/** Où en est un exercice par rapport à la dernière séance où il a été fait. */
sealed interface SessionComparison {
    /** Jamais fait avant cette séance. */
    data object FirstTime : SessionComparison

    /** Écart de la charge la plus lourde, en kilos (0 = même charge). */
    data class Weight(val deltaKg: Double) : SessionComparison
}

/**
 * Compare la charge la plus lourde de [sets] à celle de la séance précédente.
 *
 * [earlierSets] contient les séries du même exercice faites *avant* cette séance,
 * dans des séances terminées, de la plus récente à la plus ancienne. Null pour
 * un exercice sans charge : il n'y a pas de poids à comparer.
 */
fun compareWithPreviousSession(
    sets: List<SetResultEntity>,
    earlierSets: List<SetResultEntity>,
): SessionComparison? {
    val best = sets.mapNotNull { it.weightKg }.maxOrNull() ?: return null
    val previousSession = earlierSets.firstOrNull()?.exerciseSessionId
        ?: return SessionComparison.FirstTime
    val previousBest = earlierSets
        .filter { it.exerciseSessionId == previousSession }
        .mapNotNull { it.weightKg }
        .maxOrNull()
        ?: return SessionComparison.FirstTime
    return SessionComparison.Weight(best - previousBest)
}

/**
 * La charge qui résume la dernière séance d'un exercice : la plus lourde utilisée
 * ce jour-là. C'est la même règle partout — catalogue, détail, progression —
 * pour qu'un même exercice n'affiche jamais deux charges différentes.
 *
 * [sets] : les séries d'un exercice, séances terminées, de la plus récente à la plus ancienne.
 */
fun latestSessionBest(sets: List<SetResultEntity>): Double? {
    val latest = sets.firstOrNull { it.weightKg != null }?.exerciseSessionId ?: return null
    return sets.filter { it.exerciseSessionId == latest }.mapNotNull { it.weightKg }.maxOrNull()
}

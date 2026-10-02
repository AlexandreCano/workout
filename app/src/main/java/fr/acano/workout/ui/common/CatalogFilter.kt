package fr.acano.workout.ui.common

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.seed.ExerciseCatalog
import fr.acano.workout.domain.ExerciseCategory
import java.text.Normalizer

/**
 * Recherche et filtre du catalogue, partagés par l'onglet Exercices et le
 * sélecteur d'un entraînement. [category] nulle : toutes les familles.
 */
data class CatalogFilter(
    val query: String = "",
    val category: ExerciseCategory? = null,
) {
    val isActive: Boolean get() = query.isNotBlank() || category != null

    fun matches(exercise: ExerciseEntity): Boolean {
        if (category != null && exercise.category != category) return false
        val needle = query.normalizedForSearch()
        return needle.isEmpty() || needle in exercise.name.normalizedForSearch()
    }
}

/** Recherche tolérante : « presse », « Presse » et « préssé » se retrouvent. */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/**
 * Par famille, puis dans l'ordre du catalogue ; les exercices de l'utilisateur
 * rejoignent leur famille, rangés par nom, et ceux sans famille ferment la marche.
 */
val catalogueOrder: Comparator<ExerciseEntity> =
    compareBy<ExerciseEntity> { it.category?.ordinal ?: Int.MAX_VALUE }
        .thenBy { ExerciseCatalog.indexOf(it.id) }
        .thenBy { it.name.lowercase() }

/** Les familles représentées dans [exercises], dans l'ordre d'affichage : un filtre vide ne sert à rien. */
fun categoriesOf(exercises: List<ExerciseEntity>): List<ExerciseCategory> {
    val present = exercises.mapNotNullTo(mutableSetOf()) { it.category }
    return ExerciseCategory.entries.filter { it in present }
}

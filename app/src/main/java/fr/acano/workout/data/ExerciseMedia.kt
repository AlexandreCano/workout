package fr.acano.workout.data

import android.content.Context

/**
 * Associe un exercice à son animation dans `assets/exercises/`.
 *
 * Le dossier est listé une fois au démarrage : l'extension n'a donc pas d'importance
 * (.gif, .webp animé, .png…) et un fichier manquant renvoie simplement `null`,
 * ce qui déclenche l'affichage d'un visuel de remplacement sans rien casser.
 */
class ExerciseMedia(context: Context) {

    private val byExerciseId: Map<String, String> = runCatching {
        context.assets.list(DIRECTORY)
            .orEmpty()
            .filter { it.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS }
            .associateBy { it.substringBeforeLast('.') }
    }.getOrDefault(emptyMap())

    /** URI chargeable par Coil, ou `null` si aucune animation n'a été déposée. */
    fun uriFor(exerciseId: String): String? =
        byExerciseId[exerciseId]?.let { "file:///android_asset/$DIRECTORY/$it" }

    private companion object {
        const val DIRECTORY = "exercises"
        val SUPPORTED_EXTENSIONS = setOf("gif", "webp", "png", "jpg", "jpeg")
    }
}

package fr.acano.workout.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Images d'exercices choisies par l'utilisateur.
 *
 * Le sélecteur de photos ne donne qu'un accès temporaire au fichier d'origine :
 * on en garde donc une copie dans le stockage privé de l'application, qui
 * survit aux redémarrages et à la suppression de la photo dans la galerie.
 *
 * Chaque import porte un nom neuf : l'URI change avec l'image, si bien que le
 * cache de Coil ne peut jamais servir l'ancienne après un remplacement.
 */
class ExerciseImageStore(context: Context) {

    private val resolver = context.contentResolver
    private val directory = File(context.filesDir, DIRECTORY)

    /** Copie l'image désignée par [source] et renvoie le chemin absolu de la copie. */
    suspend fun import(source: Uri, exerciseId: String): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val extension = resolver.getType(source)
            ?.substringAfter('/')
            ?.takeIf { it.isNotBlank() && it.all(Char::isLetterOrDigit) }
            ?: "img"
        val target = File(directory, "${exerciseId}_${System.currentTimeMillis()}.$extension")
        val input = requireNotNull(resolver.openInputStream(source)) { "Image illisible : $source" }
        input.use { from -> target.outputStream().use { to -> from.copyTo(to) } }
        target.absolutePath
    }

    /** Supprime une copie devenue inutile. Ignore tout chemin extérieur au dossier des images. */
    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        val file = path?.let(::File) ?: return@withContext
        if (file.parentFile?.canonicalPath == directory.canonicalPath) file.delete()
    }

    private companion object {
        const val DIRECTORY = "exercise_images"
    }
}

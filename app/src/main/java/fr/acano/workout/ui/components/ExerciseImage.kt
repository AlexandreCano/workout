package fr.acano.workout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import fr.acano.workout.WorkoutApp
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.ui.theme.WorkoutMotion

/**
 * Animation d'un exercice.
 *
 * L'image choisie par l'utilisateur passe avant l'animation fournie dans
 * `assets/exercises/`. Tant qu'il n'y a ni l'une ni l'autre — ou si son
 * décodage échoue — un visuel de remplacement prend sa place : l'application
 * reste parfaitement utilisable sans aucun média. L'apparition est fondue pour
 * éviter le saut visuel quand un GIF finit de décoder.
 *
 * L'image est purement décorative : elle est retirée de l'arbre d'accessibilité
 * pour ne pas polluer la lecture TalkBack de l'écran de séance.
 */
@Composable
fun ExerciseImage(
    exercise: ExerciseEntity,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val context = LocalContext.current
    val media = remember { (context.applicationContext as WorkoutApp).container.exerciseMedia }
    val uri = remember(exercise.id, exercise.imagePath) {
        exercise.imagePath?.let { "file://$it" } ?: media.uriFor(exercise.id)
    }
    ExerciseImage(model = uri, modifier = modifier, shape = shape)
}

/** Affiche une image quelconque (fichier, `content://`…) avec le même cadre et le même repli. */
@Composable
fun ExerciseImage(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val context = LocalContext.current
    val uri = model

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        ExercisePlaceholder()

        if (uri != null) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context).data(uri).build(),
            )
            val state by painter.state.collectAsState()

            AnimatedVisibility(
                visible = state is AsyncImagePainter.State.Success,
                enter = fadeIn(WorkoutMotion.effects()),
                exit = fadeOut(WorkoutMotion.effects()),
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ExercisePlaceholder() {
    Icon(
        imageVector = Icons.Rounded.FitnessCenter,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.fillMaxSize(0.26f),
    )
}

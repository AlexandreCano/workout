package fr.acano.workout.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import fr.acano.workout.WorkoutApp

/**
 * Animation d'un exercice. Tant qu'aucun fichier n'a été déposé dans `assets/exercises/`
 * — ou si son décodage échoue — un visuel de remplacement prend sa place :
 * l'application reste parfaitement utilisable sans aucun média.
 */
@Composable
fun ExerciseImage(
    exerciseId: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
) {
    val context = LocalContext.current
    val media = remember { (context.applicationContext as WorkoutApp).container.exerciseMedia }
    val uri = remember(exerciseId) { media.uriFor(exerciseId) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (uri == null) {
            Placeholder()
            return@Box
        }

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context).data(uri).build(),
        )
        val state by painter.state.collectAsState()

        when (state) {
            is AsyncImagePainter.State.Success -> Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Placeholder()
        }
    }
}

@Composable
private fun Placeholder() {
    Icon(
        imageVector = Icons.Rounded.FitnessCenter,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxSize(0.28f),
    )
}

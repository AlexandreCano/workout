package fr.acano.workout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSpacing = staticCompositionLocalOf { WorkoutSpacing() }
private val LocalEmphasis = staticCompositionLocalOf { WorkoutEmphasis() }

/**
 * Tokens propres à l'application, en complément de `MaterialTheme`.
 * On passe par un CompositionLocal plutôt que par des constantes globales
 * pour rester cohérent avec la façon dont Material expose ses propres tokens.
 */
object WorkoutTheme {
    val spacing: WorkoutSpacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current

    val emphasis: WorkoutEmphasis
        @Composable @ReadOnlyComposable get() = LocalEmphasis.current
}

/**
 * Thème de l'application.
 *
 * Note : `MaterialExpressiveTheme` et les fabriques `MotionScheme.expressive()`
 * existent dans Material 3 1.4.0 mais y sont encore `internal`. On utilise donc
 * `MaterialTheme`, et les ressorts Expressive sont définis dans [WorkoutMotion],
 * qui lit `MaterialTheme.motionScheme` quand c'est possible.
 *
 * La couleur dynamique est volontairement désactivée : l'identité visuelle de
 * l'application repose sur son accent lime, pas sur le fond d'écran du téléphone.
 */
@Composable
fun WorkoutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSpacing provides WorkoutSpacing(),
        LocalEmphasis provides WorkoutEmphasis(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) WorkoutDarkColorScheme else WorkoutLightColorScheme,
            shapes = WorkoutShapes,
            typography = WorkoutTypography,
            content = content,
        )
    }
}

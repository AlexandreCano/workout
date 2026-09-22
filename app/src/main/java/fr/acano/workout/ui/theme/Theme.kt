package fr.acano.workout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnLime,
    primaryContainer = LimeDark,
    onPrimaryContainer = OnLime,
    secondary = Lime,
    onSecondary = OnLime,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = Outline,
    error = Danger,
    onError = OnLime,
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Danger,
)

/** Les chiffres consultés d'un coup d'œil pendant l'effort : gros, condensés, tabulaires. */
private val WorkoutTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            letterSpacing = (-2).sp,
        ),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    )
}

val NumericStyle: TextStyle
    @Composable get() = MaterialTheme.typography.displayLarge

@Composable
fun WorkoutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WorkoutTypography,
        content = content,
    )
}

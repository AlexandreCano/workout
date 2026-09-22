package fr.acano.workout.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Palette dérivée d'une seule teinte source : un vert-jaune énergique.
 *
 * Les neutres sont légèrement teintés vers le vert plutôt que purement gris :
 * sur un écran OLED sombre, un gris neutre à côté d'un accent lime paraît violacé.
 * Les tons suivent l'échelle tonale Material 3 (le nombre en commentaire).
 */

// Accent principal — vert-jaune
private val Lime90 = Color(0xFFDDFF9C)
private val Lime80 = Color(0xFFC0EC6B)
private val Lime60 = Color(0xFF9ACB3F)
private val Lime40 = Color(0xFF4C6A00)
private val Lime30 = Color(0xFF394F00)
private val Lime20 = Color(0xFF263500)
private val Lime10 = Color(0xFF141F00)

// Accent secondaire — vert désaturé, pour les surfaces récessives
private val Sage90 = Color(0xFFDFE8C8)
private val Sage80 = Color(0xFFC3CCAD)
private val Sage40 = Color(0xFF5A6243)
private val Sage30 = Color(0xFF434B2D)
private val Sage20 = Color(0xFF2D3418)
private val Sage10 = Color(0xFF191F06)

// Accent tertiaire — ambre, réservé à l'étoile et aux récompenses
private val Amber90 = Color(0xFFFFDDB3)
private val Amber80 = Color(0xFFFFD28E)
private val Amber40 = Color(0xFF7D5700)
private val Amber30 = Color(0xFF5F4100)
private val Amber20 = Color(0xFF452B00)
private val Amber10 = Color(0xFF2A1700)

// Neutres teintés vert
private val N4 = Color(0xFF0A0E08)
private val N6 = Color(0xFF101410)
private val N10 = Color(0xFF181C16)
private val N12 = Color(0xFF1C211A)
private val N17 = Color(0xFF272C24)
private val N22 = Color(0xFF32382F)
private val N24 = Color(0xFF363B33)
private val N90 = Color(0xFFE3E5DA)
private val N95 = Color(0xFFF1F4E7)
private val N98 = Color(0xFFFAFCF0)
private val N100 = Color(0xFFFFFFFF)
private val N20 = Color(0xFF2D322A)
private val N10Light = Color(0xFF191D16)

private val NV80 = Color(0xFFC3CBB8)
private val NV60 = Color(0xFF8D9583)
private val NV50 = Color(0xFF747C6B)
private val NV30 = Color(0xFF434A3D)
private val NV90 = Color(0xFFDFE7D3)

// Erreur — l'échelle rouge de Material 3, inchangée par la teinte source
private val Red80 = Color(0xFFFFB4AB)
private val Red40 = Color(0xFFBA1A1A)
private val Red30 = Color(0xFF93000A)
private val Red20 = Color(0xFF690005)
private val Red90 = Color(0xFFFFDAD6)

/** Mode sombre : le mode de référence de l'application. */
val WorkoutDarkColorScheme = darkColorScheme(
    primary = Lime80,
    onPrimary = Lime20,
    primaryContainer = Lime30,
    onPrimaryContainer = Lime90,
    inversePrimary = Lime40,

    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,

    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,

    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,

    background = N6,
    onBackground = N90,
    surface = N6,
    onSurface = N90,
    onSurfaceVariant = NV80,

    surfaceContainerLowest = N4,
    surfaceContainerLow = N10,
    surfaceContainer = N12,
    surfaceContainerHigh = N17,
    surfaceContainerHighest = N22,
    surfaceDim = N6,
    surfaceBright = N24,

    outline = NV60,
    outlineVariant = NV30,

    inverseSurface = N90,
    inverseOnSurface = N20,
    scrim = Color(0xFF000000),
)

/** Mode clair : l'application privilégie le sombre, mais suit le réglage système. */
val WorkoutLightColorScheme = lightColorScheme(
    primary = Lime40,
    onPrimary = N100,
    primaryContainer = Lime90,
    onPrimaryContainer = Lime10,
    inversePrimary = Lime80,

    secondary = Sage40,
    onSecondary = N100,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,

    tertiary = Amber40,
    onTertiary = N100,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,

    error = Red40,
    onError = N100,
    errorContainer = Red90,
    onErrorContainer = Red20,

    background = N98,
    onBackground = N10Light,
    surface = N98,
    onSurface = N10Light,
    onSurfaceVariant = Color(0xFF44493F),

    surfaceContainerLowest = N100,
    surfaceContainerLow = N95,
    surfaceContainer = Color(0xFFEFF2E5),
    surfaceContainerHigh = Color(0xFFE9ECDF),
    surfaceContainerHighest = Color(0xFFE3E6DA),
    surfaceDim = Color(0xFFDADDD0),
    surfaceBright = N98,

    outline = NV50,
    outlineVariant = NV90,

    inverseSurface = Color(0xFF2E322A),
    inverseOnSurface = N95,
    scrim = Color(0xFF000000),
)

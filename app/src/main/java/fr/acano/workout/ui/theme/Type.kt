package fr.acano.workout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Échelle typographique Material 3, valeurs de la spécification.
 *
 * Deux écarts assumés, tous deux motivés par l'usage en salle :
 *  - le tracking des styles Display est resserré (les très gros chiffres respirent
 *    déjà assez, un tracking positif les délite) ;
 *  - les chiffres sont **tabulaires** partout où une valeur change en place
 *    (chrono, charge), sinon le compteur « saute » à chaque seconde.
 */

private val Plain = FontFamily.SansSerif

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Plain,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TightLineHeight,
)

val WorkoutTypography = Typography(
    displayLarge = style(57, 64, FontWeight.Bold, -1.5),
    displayMedium = style(45, 52, FontWeight.Bold, -1.0),
    displaySmall = style(36, 44, FontWeight.Bold, -0.5),

    headlineLarge = style(32, 40, FontWeight.Bold, -0.5),
    headlineMedium = style(28, 36, FontWeight.Bold, -0.25),
    headlineSmall = style(24, 32, FontWeight.SemiBold),

    titleLarge = style(22, 28, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold, 0.15),
    titleSmall = style(14, 20, FontWeight.Medium, 0.1),

    bodyLarge = style(16, 24, FontWeight.Normal, 0.5),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.25),
    bodySmall = style(12, 16, FontWeight.Normal, 0.4),

    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.5),
    labelSmall = style(11, 16, FontWeight.Medium, 0.5),
)

/**
 * Styles « emphasized » de l'update Expressive. Material 3 1.4.0 les embarque
 * mais ne les expose pas encore publiquement : on les redéfinit ici, à la même
 * échelle et avec la graisse supérieure prévue par la spécification.
 */
@Suppress("unused")
class WorkoutEmphasis internal constructor() {

    /** Chiffres géants qui changent en place : chrono, charge. */
    val counter: TextStyle = TextStyle(
        fontFamily = Plain,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 100.sp,
        letterSpacing = (-4).sp,
        fontFeatureSettings = TABULAR,
        lineHeightStyle = TightLineHeight,
    )

    /** Valeur numérique mise en avant dans un bloc (poids, durée). */
    val metric: TextStyle = TextStyle(
        fontFamily = Plain,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = TABULAR,
        lineHeightStyle = TightLineHeight,
    )

    /** Valeur numérique dans une liste (dernière charge d'un exercice). */
    val metricSmall: TextStyle = TextStyle(
        fontFamily = Plain,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = TABULAR,
        lineHeightStyle = TightLineHeight,
    )

    /** Titre d'exercice sur l'écran de séance : lisible d'un coup d'œil. */
    val exerciseName: TextStyle = style(34, 38, FontWeight.ExtraBold, -0.8)

    /** Étiquette de section, en capitales espacées. */
    val overline: TextStyle = style(12, 16, FontWeight.Bold, 1.4)

    /** Libellé des boutons principaux. */
    val action: TextStyle = style(17, 22, FontWeight.Bold, 0.6)

    private companion object {
        const val TABULAR = "tnum"
    }
}

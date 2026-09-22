package fr.acano.workout.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Mouvement de l'application, en ressorts plutôt qu'en courbes à durée fixe.
 *
 * Material 3 Expressive remplace l'easing par une physique de ressort : une
 * animation interrompue repart de sa vitesse courante au lieu de sauter.
 * `MotionScheme` porte ces tokens dans Material 3 1.4.0 mais y est encore
 * `internal` — on redéfinit donc ici le schéma « expressive » avec les valeurs
 * de la spécification, à remplacer par `MaterialTheme.motionScheme` dès que
 * l'API sera publique.
 *
 * Règle d'usage : **spatial** pour ce qui bouge ou change de taille,
 * **effects** pour ce qui change d'opacité ou de couleur. Un effet ne rebondit
 * jamais (amortissement 1.0), sinon la couleur « dépasse » et scintille.
 */
object WorkoutMotion {

    /** Déplacement perçu comme immédiat : pression d'une cible tactile. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Déplacement courant : apparition d'un bloc, changement de disposition. */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Déplacement ample : bascule vers l'écran de chronomètre. */
    fun <T> slowSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 200f)

    /** Fondu ou changement de couleur rapide. */
    fun <T> fastEffects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f)

    /** Fondu ou changement de couleur courant. */
    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)

    /** Rebond marqué, réservé aux moments de récompense (l'étoile de fin de séance). */
    fun <T> celebratory(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.4f, stiffness = 260f)
}

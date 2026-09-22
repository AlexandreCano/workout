package fr.acano.workout.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Système d'espacement 8dp recommandé par l'update Material 3 de Google I/O 2026.
 * Toutes les marges et gouttières de l'application viennent d'ici : aucune valeur
 * en dur dans les écrans.
 */
@Suppress("unused")
class WorkoutSpacing internal constructor() {
    /** 4dp — séparation interne d'un même bloc (label ↔ valeur). */
    val xs: Dp = 4.dp

    /** 8dp — gouttière entre éléments liés. */
    val sm: Dp = 8.dp

    /** 12dp — respiration interne d'un composant. */
    val md: Dp = 12.dp

    /** 16dp — padding standard d'un conteneur. */
    val lg: Dp = 16.dp

    /** 24dp — marge horizontale des écrans. */
    val xl: Dp = 24.dp

    /** 32dp — séparation entre deux sections. */
    val xxl: Dp = 32.dp

    /** 48dp — respiration d'un écran « héros ». */
    val xxxl: Dp = 48.dp

    /** Cible tactile minimale confortable en pleine série. */
    val touchTarget: Dp = 56.dp

    /** Hauteur du bouton d'action principal. */
    val primaryActionHeight: Dp = 72.dp
}

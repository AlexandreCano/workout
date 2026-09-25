package fr.acano.workout.ui.common

import androidx.compose.runtime.compositionLocalOf
import fr.acano.workout.domain.WeightUnit

/**
 * Unité de poids choisie dans les réglages, fournie une fois à la racine de
 * l'interface : chaque écran qui affiche une charge la lit sans avoir à la
 * faire transiter par son ViewModel.
 */
val LocalWeightUnit = compositionLocalOf { WeightUnit.KG }

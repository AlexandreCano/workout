package fr.acano.workout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Échelle de formes Material 3, décalée d'un cran vers le haut.
 *
 * L'update Expressive introduit des tokens « increased » (20 / 32 / 48 dp) que
 * Material 3 1.4.0 garde internes. On obtient le même effet en montant les cinq
 * emplacements publics : des formes plus généreuses, cohérentes entre elles.
 */
val WorkoutShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

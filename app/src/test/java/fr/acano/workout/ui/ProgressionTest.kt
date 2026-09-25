package fr.acano.workout.ui

import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.home.buildProgression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val catalogue = mapOf(
        "pec_deck" to ExerciseEntity("pec_deck", "Pec Deck", ExerciseKind.WEIGHTED_REPS),
        "custom_x" to ExerciseEntity("custom_x", "Supprimé", ExerciseKind.WEIGHTED_REPS, isCustom = true, archivedAt = 1),
    )
    private var clock = 1_000L

    /** Une séance = une étape ; les séries sont données de la plus ancienne à la plus récente. */
    private fun session(step: Long, vararg weights: Double, exercise: String = "pec_deck") =
        weights.mapIndexed { i, w -> SetResultEntity(0, step, exercise, i + 1, w, 10, null, clock++) }

    /** La requête renvoie les séries de la plus récente à la plus ancienne. */
    private fun progression(vararg sessions: List<SetResultEntity>) =
        buildProgression(catalogue, sessions.toList().flatten().sortedByDescending { it.completedAt })

    @Test
    fun `changer de charge pendant une seule seance n est pas une progression`() {
        val line = progression(session(1, 40.0, 42.5, 45.0)).single()
        assertEquals(45.0, line.weightKg, 0.0)
        assertEquals(0, line.trend)
    }

    @Test
    fun `une seance plus lourde que la precedente est une progression`() {
        val line = progression(session(1, 40.0, 40.0), session(2, 40.0, 42.5)).single()
        assertEquals(42.5, line.weightKg, 0.0)
        assertEquals(1, line.trend)
    }

    @Test
    fun `une seance plus legere que la precedente est une baisse`() {
        assertEquals(-1, progression(session(1, 45.0), session(2, 42.5)).single().trend)
    }

    @Test
    fun `deux seances a la meme charge ne montrent aucune tendance`() {
        assertEquals(0, progression(session(1, 40.0, 42.5), session(2, 42.5, 40.0)).single().trend)
    }

    @Test
    fun `un exercice supprime n apparait pas`() {
        assertTrue(progression(session(1, 20.0, exercise = "custom_x")).isEmpty())
    }
}

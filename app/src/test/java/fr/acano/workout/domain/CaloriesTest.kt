package fr.acano.workout.domain

import fr.acano.workout.data.db.entity.SetResultEntity
import fr.acano.workout.data.seed.ExerciseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaloriesTest {

    private val year = 2026
    private val full = UserProfile(sex = Sex.MALE, weightKg = 80.0, heightCm = 180, birthYear = 1996)

    @Test
    fun `avec le poids seul on applique la formule standard du compendium`() {
        // 3,5 MET × 3,5 × 80 kg / 200 = 4,9 kcal/min
        assertEquals(4.9, Calories.kcalPerMinute(3.5, UserProfile(weightKg = 80.0), year)!!, 1e-9)
    }

    @Test
    fun `le profil complet corrige le met par le metabolisme de repos`() {
        // Mifflin-St Jeor : 10×80 + 6,25×180 − 5×30 + 5 = 1780 kcal/jour ≈ 3,09 ml/kg/min.
        assertEquals(3.090, Calories.restingMetabolism(full, year)!!, 1e-3)
        assertEquals(5.55, Calories.kcalPerMinute(3.5, full, year)!!, 0.01)
        // Même gabarit, métabolisme de repos plus bas : le MET corrigé monte.
        val female = full.copy(sex = Sex.FEMALE)
        assertEquals(true, Calories.kcalPerMinute(3.5, female, year)!! > 5.55)
    }

    @Test
    fun `sans poids aucune estimation`() {
        assertNull(Calories.kcalPerMinute(3.5, UserProfile(heightCm = 180), year))
    }

    private fun set(step: Long, at: Long, duration: Int? = null) =
        SetResultEntity(0, step, "x", 1, null, null, duration, at)

    @Test
    fun `la duree d un exercice court depuis la fin du precedent, repos compris`() {
        val start = 0L
        val min = 60_000L
        // Étape A finie à 10 min, étape B de 12 à 20 min : A = 10 min, B = 10 min (20 − 10).
        val a = listOf(set(1, 4 * min), set(1, 10 * min))
        val b = listOf(set(2, 12 * min), set(2, 20 * min))
        assertEquals(listOf(10.0, 10.0), Calories.exerciseMinutes(start, listOf(a, b)))
        // L'ordre réel compte, pas l'ordre prévu : B faite avant A.
        val bFirst = listOf(set(2, 5 * min))
        val aAfter = listOf(set(1, 9 * min))
        assertEquals(listOf(4.0, 5.0), Calories.exerciseMinutes(start, listOf(aAfter, bFirst)))
    }

    @Test
    fun `une pause oubliee est plafonnee et une serie chronometree compte au moins sa duree`() {
        val hour = 3_600_000L
        assertEquals(listOf(30.0), Calories.exerciseMinutes(0, listOf(listOf(set(1, 2 * hour)))))
        // 5 min de vélo enregistrées 10 s après le début : 5 min, pas 10 s.
        assertEquals(listOf(5.0), Calories.exerciseMinutes(0, listOf(listOf(set(1, 10_000, duration = 300)))))
    }

    @Test
    fun `seuls les exercices de l application sont estimes`() {
        val min = 60_000L
        val kcal = Calories.perExercise(
            sessionStartedAt = 0,
            steps = listOf(
                "chest_press" to listOf(set(1, 10 * min)),
                "custom_1" to listOf(set(2, 20 * min)),
            ),
            profile = UserProfile(weightKg = 80.0),
            currentYear = year,
        )
        assertEquals(49.0, kcal[0]!!, 1e-9) // 10 min × 4,9 kcal/min
        assertNull(kcal[1])
    }

    private fun met(id: String) = Calories.metFor(ExerciseCatalog[id]!!)

    @Test
    fun `chaque exercice du catalogue a une estimation`() {
        ExerciseCatalog.entries.forEach { assertEquals(it.id, true, Calories.hasEstimate(it.id)) }
    }

    @Test
    fun `les exercices d origine gardent leur met`() {
        assertEquals(5.5, met("bike_warmup"), 0.0)
        assertEquals(3.5, met("chest_press"), 0.0)
        assertEquals(3.5, met("lying_leg_curl"), 0.0)
        assertEquals(5.0, met("leg_press"), 0.0)
        assertEquals(3.8, met("plank"), 0.0)
        assertEquals(1.5, met("stomach_vacuum"), 0.0)
    }

    @Test
    fun `le met suit la famille de l exercice`() {
        assertEquals(5.0, met("barbell_back_squat"), 0.0)
        assertEquals(3.5, met("leg_extension"), 0.0)
        assertEquals(3.8, met("push_up"), 0.0)
        assertEquals(7.0, met("rowing_machine"), 0.0)
        assertEquals(11.8, met("jump_rope"), 0.0)
    }
}

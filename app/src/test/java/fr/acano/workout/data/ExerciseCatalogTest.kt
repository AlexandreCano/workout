package fr.acano.workout.data

import fr.acano.workout.data.seed.ExerciseCatalog
import fr.acano.workout.domain.Equipment
import fr.acano.workout.domain.ExerciseCategory
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.domain.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le catalogue JSON est lu tel qu'il est livré : ces tests échouent si le fichier est incohérent. */
class ExerciseCatalogTest {

    @Test
    fun `le catalogue se lit entierement`() {
        // Une valeur inconnue (muscle, matériel…) ferait échouer la lecture elle-même.
        assertTrue(ExerciseCatalog.entries.size > 200)
    }

    @Test
    fun `aucun identifiant n est duplique et tous sont des slugs`() {
        val ids = ExerciseCatalog.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue("identifiant invalide : $it", it.matches(Regex("[a-z0-9_]+"))) }
    }

    @Test
    fun `les exercices des premieres versions sont toujours la`() {
        // L'historique et les entraînements existants y font référence par leur identifiant,
        // directement ou à travers un renommage.
        listOf(
            "bike_warmup", "chest_press", "pec_deck", "lat_pulldown", "seated_row", "leg_press",
            "leg_curl", "leg_extension", "calf_raise", "plank", "stomach_vacuum",
        ).forEach { id ->
            val current = ExerciseCatalog.renamedIds[id] ?: id
            assertNotNull("$id absent du catalogue", ExerciseCatalog[current])
        }
        // Un ancien identifiant renommé ne doit pas revenir dans le catalogue.
        ExerciseCatalog.renamedIds.keys.forEach { assertNull(ExerciseCatalog[it]) }
    }

    @Test
    fun `chaque exercice a un nom dans les deux langues`() {
        ExerciseCatalog.entries.forEach {
            assertTrue(it.id, it.nameFr.isNotBlank() && it.nameEn.isNotBlank())
        }
        val press = ExerciseCatalog["seated_row"]!!
        assertEquals("Tirage horizontal assis", press.name("fr"))
        assertEquals("Seated Cable Row", press.name("en"))
        // Toute autre langue retombe sur l'anglais, comme le reste de l'interface.
        assertEquals("Seated Cable Row", press.name("de"))
    }

    @Test
    fun `les details du catalogue arrivent jusqu a l exercice`() {
        val entity = ExerciseCatalog["chest_press"]!!.toEntity()
        assertEquals(ExerciseCategory.CHEST, entity.category)
        assertEquals(Muscle.CHEST, entity.primaryMuscle)
        assertEquals(listOf(Muscle.TRICEPS, Muscle.FRONT_DELTS), entity.secondaryMuscles)
        assertEquals(Equipment.MACHINE, entity.equipment)
        assertEquals(ExerciseKind.WEIGHTED_REPS, entity.kind)
    }

    @Test
    fun `les modes de suivi du catalogue deviennent des types d exercice`() {
        assertEquals(ExerciseKind.TIMED, ExerciseCatalog["plank"]!!.kind)
        assertEquals(ExerciseKind.WEIGHTED_TIMED, ExerciseCatalog["weighted_plank"]!!.kind)
        assertEquals(ExerciseKind.WEIGHTED_DISTANCE, ExerciseCatalog["farmer_carry"]!!.kind)
        assertEquals(ExerciseKind.TIMED_DISTANCE, ExerciseCatalog["treadmill"]!!.kind)
    }

    @Test
    fun `la presse et le traineau ont un pas de charge adapte`() {
        assertEquals(5.0, ExerciseCatalog["leg_press"]!!.toEntity().weightStepKg, 0.0)
        assertEquals(10.0, ExerciseCatalog["sled_push"]!!.toEntity().weightStepKg, 0.0)
        assertEquals(2.5, ExerciseCatalog["chest_press"]!!.toEntity().weightStepKg, 0.0)
    }

    @Test
    fun `le catalogue est range par famille`() {
        // L'ordre du fichier sert d'ordre d'affichage à l'intérieur d'une famille.
        assertTrue(ExerciseCatalog.indexOf("lying_leg_curl") < ExerciseCatalog.indexOf("seated_leg_curl"))
        assertEquals(Int.MAX_VALUE, ExerciseCatalog.indexOf("custom_1"))
    }
}

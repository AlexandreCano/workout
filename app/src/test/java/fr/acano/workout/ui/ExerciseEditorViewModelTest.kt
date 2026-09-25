package fr.acano.workout.ui

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.data.ExerciseImageStore
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.exercises.ExerciseEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExerciseEditorViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: WorkoutDatabase
    private lateinit var repository: WorkoutRepository
    private val images = ExerciseImageStore(context)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database.exerciseDao(), database.workoutDao(), database.customWorkoutDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** Comme l'écran, on n'agit qu'une fois l'exercice chargé. */
    private suspend fun editor(id: String? = null, clock: Long = 42L) =
        ExerciseEditorViewModel(repository, images, id) { clock }
            .also { vm -> vm.state.first { !it.isLoading } }

    @Test
    fun `un exercice ne porte que son nom son type et son pas de charge`() = runTest {
        val vm = editor()
        vm.setName("Planche latérale")
        vm.setKind(ExerciseKind.TIMED)
        vm.setWeightStep(5.0)

        assertTrue(vm.save())

        val saved = repository.exercise("custom_42")!!
        assertTrue(saved.isCustom)
        assertEquals(ExerciseKind.TIMED, saved.kind)
        assertEquals(5.0, saved.weightStepKg, 0.0)
    }

    @Test
    fun `supprimer l exercice efface aussi son image`() = runTest {
        val create = editor()
        create.setName("Rowing")
        create.pickImage(Uri.fromFile(photo("d.png")))
        create.save()
        val copy = repository.exercise("custom_42")!!.imagePath!!

        editor(id = "custom_42").delete()

        assertTrue(repository.exercise("custom_42")!!.isArchived)
        assertFalse(File(copy).exists())
    }

    @Test
    fun `sans nom on n enregistre rien`() = runTest {
        val vm = editor()
        assertFalse(vm.save())
        assertNull(repository.exercise("custom_42"))
    }

    @Test
    fun `l image est copiee et l ancienne supprimee au remplacement`() = runTest {
        val first = photo("a.png")
        val create = editor()
        create.setName("Rowing")
        create.pickImage(Uri.fromFile(first))
        create.save()
        val firstCopy = repository.exercise("custom_42")!!.imagePath!!
        assertTrue(File(firstCopy).exists())
        // Supprimer la photo d'origine ne doit rien changer : l'application a sa copie.
        first.delete()
        assertTrue(File(firstCopy).exists())

        val edit = editor(id = "custom_42")
        edit.pickImage(Uri.fromFile(photo("b.png")))
        edit.save()

        val secondCopy = repository.exercise("custom_42")!!.imagePath
        assertNotNull(secondCopy)
        assertTrue(secondCopy != firstCopy)
        assertFalse(File(firstCopy).exists())
    }

    @Test
    fun `retirer l image efface la copie`() = runTest {
        val create = editor()
        create.setName("Rowing")
        create.pickImage(Uri.fromFile(photo("c.png")))
        create.save()
        val copy = repository.exercise("custom_42")!!.imagePath!!

        val edit = editor(id = "custom_42")
        edit.removeImage()
        edit.save()

        assertNull(repository.exercise("custom_42")!!.imagePath)
        assertFalse(File(copy).exists())
    }

    private fun photo(name: String): File =
        File(context.cacheDir, name).apply { writeBytes(byteArrayOf(1, 2, 3)) }
}

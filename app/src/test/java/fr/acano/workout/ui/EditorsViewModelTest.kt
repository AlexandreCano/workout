package fr.acano.workout.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.db.entity.ExerciseEntity
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.domain.ExerciseKind
import fr.acano.workout.ui.exercises.ExercisesUiState
import fr.acano.workout.ui.exercises.ExercisesViewModel
import fr.acano.workout.ui.workouts.WorkoutEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorsViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: WorkoutDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .addCallback(WorkoutDatabase.callback)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database.exerciseDao(), database.workoutDao(), database.customWorkoutDao())
        repository.ensureSeeded()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun upperBodyEditor(): WorkoutEditorViewModel {
        val id = repository.observeCustomWorkouts().first().first().workout.id
        return WorkoutEditorViewModel(repository, id).also { vm -> vm.state.first { !it.isLoading } }
    }

    @Test
    fun `un entrainement tout juste ouvert n a pas de modification en attente`() = runTest {
        assertFalse(upperBodyEditor().state.value.hasUnsavedChanges)
    }

    @Test
    fun `retirer un exercice se remet a sa place en annulant`() = runTest {
        val vm = upperBodyEditor()
        val before = vm.state.value.steps.map { it.exercise.id }
        val chest = vm.state.value.steps.first { it.exercise.id == "chest_press" }

        assertEquals("Chest Press", vm.remove(chest.key))
        assertTrue(vm.state.value.hasUnsavedChanges)

        vm.undoRemove()
        assertEquals(before, vm.state.value.steps.map { it.exercise.id })
        // Revenu à l'identique : plus rien à enregistrer.
        assertFalse(vm.state.value.hasUnsavedChanges)
    }

    @Test
    fun `renommer compte comme une modification`() = runTest {
        val vm = upperBodyEditor()
        vm.setName("Haut du corps bis")
        assertTrue(vm.state.value.hasUnsavedChanges)
    }

    @Test
    fun `la recherche ignore les accents et les majuscules`() = runTest {
        repository.saveCustomExercise(ExerciseEntity("custom_1", "Développé incliné", ExerciseKind.WEIGHTED_REPS, isCustom = true))
        val vm = ExercisesViewModel(repository)
        backgroundScope.launch { vm.state.collect {} }
        suspend fun search(q: String): ExercisesUiState {
            vm.setQuery(q)
            return vm.state.first { it.query == q }
        }

        assertEquals(listOf("leg_press"), search("PRESSE").builtIn.map { it.exercise.id })
        assertEquals(listOf("custom_1"), search("developpe").custom.map { it.exercise.id })
        val none = search("zzz")
        assertTrue(none.builtIn.isEmpty() && none.custom.isEmpty())
    }
}

package fr.acano.workout.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.acano.workout.data.db.WorkoutDatabase
import fr.acano.workout.data.repository.WorkoutRepository
import fr.acano.workout.ui.history.HistoryUiState
import fr.acano.workout.ui.history.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoryViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: WorkoutDatabase
    private lateinit var repository: WorkoutRepository
    private var clock = 0L
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 9, 25)

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .addCallback(WorkoutDatabase.callback)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database.exerciseDao(), database.workoutDao(), database.customWorkoutDao()) { clock }
        repository.ensureSeeded()
        // Deux séances le 3 septembre, une le 18, une le 20 août.
        listOf(
            LocalDateTime.of(2026, 9, 3, 9, 0),
            LocalDateTime.of(2026, 9, 3, 18, 0),
            LocalDateTime.of(2026, 9, 18, 12, 0),
            LocalDateTime.of(2026, 8, 20, 12, 0),
        ).forEach { at ->
            clock = at.toInstant(zone).toEpochMilli()
            val workout = repository.observeCustomWorkouts().first().first().workout.id
            repository.finishSession(repository.startSession(workout))
        }
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** Garde un abonné actif, comme l'écran, et attend que l'historique soit chargé. */
    private suspend fun TestScope.loaded(vm: HistoryViewModel): HistoryUiState {
        backgroundScope.launch { vm.state.collect {} }
        return vm.state.first { !it.isLoading && it.totalSessions == 4 }
    }

    private fun viewModel() = HistoryViewModel(repository, zone = zone) { today }

    /**
     * L'état se recalcule de façon asynchrone (en partie sur le fil de Room) :
     * on attend l'état voulu plutôt que de lire `value` juste après une action.
     */
    private suspend fun HistoryViewModel.awaitState(predicate: (HistoryUiState) -> Boolean): HistoryUiState =
        state.first(predicate)

    @Test
    fun `le calendrier s ouvre sur le mois courant avec les jours d entrainement`() = runTest {
        val state = loaded(viewModel())
        assertEquals(YearMonth.of(2026, 9), state.month)
        assertEquals(2, state.sessionsPerDay[LocalDate.of(2026, 9, 3)])
        assertEquals(1, state.sessionsPerDay[LocalDate.of(2026, 9, 18)])
        assertEquals(3, state.sessionsInMonth)
        assertEquals(4, state.sessions.size)
    }

    @Test
    fun `choisir un jour filtre la liste puis le rechoisir la retablit`() = runTest {
        val vm = viewModel()
        loaded(vm)
        vm.toggleDay(LocalDate.of(2026, 9, 3))
        assertEquals(2, vm.awaitState { it.selectedDay != null }.sessions.size)
        vm.toggleDay(LocalDate.of(2026, 9, 3))
        val cleared = vm.awaitState { it.selectedDay == null }
        assertEquals(4, cleared.sessions.size)
    }

    @Test
    fun `un jour sans seance n est pas selectionnable`() = runTest {
        val vm = viewModel()
        loaded(vm)
        vm.toggleDay(LocalDate.of(2026, 9, 4))
        assertNull(vm.state.value.selectedDay)
    }

    @Test
    fun `la navigation va du premier mois d entrainement au mois courant`() = runTest {
        val vm = viewModel()
        val start = loaded(vm)
        assertTrue(start.canGoToPreviousMonth)
        assertFalse(start.canGoToNextMonth)

        vm.previousMonth()
        val august = vm.awaitState { it.month == YearMonth.of(2026, 8) }
        assertEquals(1, august.sessionsInMonth)
        assertFalse(august.canGoToPreviousMonth)

        // Le mois courant est une borne : pas de mois futur.
        vm.nextMonth(); vm.nextMonth()
        assertEquals(YearMonth.of(2026, 9), vm.awaitState { it.month == YearMonth.of(2026, 9) }.month)
        assertFalse(vm.state.value.canGoToNextMonth)
    }

    @Test
    fun `changer de mois oublie le jour choisi`() = runTest {
        val vm = viewModel()
        loaded(vm)
        vm.toggleDay(LocalDate.of(2026, 9, 18))
        vm.awaitState { it.selectedDay != null }
        vm.previousMonth()
        val august = vm.awaitState { it.month == YearMonth.of(2026, 8) }
        assertNull(august.selectedDay)
        assertEquals(4, august.sessions.size)
    }
}

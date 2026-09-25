package fr.acano.workout.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import fr.acano.workout.R
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.acano.workout.di.appContainer
import fr.acano.workout.di.containerViewModel
import fr.acano.workout.ui.common.LocalWeightUnit
import fr.acano.workout.ui.settings.SettingsScreen
import fr.acano.workout.ui.exercises.ExerciseDetailScreen
import fr.acano.workout.ui.exercises.ExerciseEditorScreen
import fr.acano.workout.ui.exercises.ExerciseEditorViewModel
import fr.acano.workout.ui.exercises.ExerciseDetailViewModel
import fr.acano.workout.ui.exercises.ExercisesScreen
import fr.acano.workout.ui.exercises.ExercisesViewModel
import fr.acano.workout.ui.history.HistoryScreen
import fr.acano.workout.ui.history.HistoryViewModel
import fr.acano.workout.ui.history.SessionDetailScreen
import fr.acano.workout.ui.history.SessionDetailViewModel
import fr.acano.workout.ui.home.HomeScreen
import fr.acano.workout.ui.home.HomeViewModel
import fr.acano.workout.ui.session.ActiveSessionScreen
import fr.acano.workout.ui.session.ActiveSessionViewModel
import fr.acano.workout.ui.session.SessionSummaryScreen
import fr.acano.workout.ui.workouts.WorkoutEditorScreen
import fr.acano.workout.ui.workouts.WorkoutEditorViewModel

private data class Tab(
    val route: Route,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Icône pleine à l'état actif, contour au repos : c'est la convention Material 3
 * et c'est ce qui rend l'onglet courant identifiable sans lire le libellé.
 */
private val tabs = listOf(
    Tab(Route.Home, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Route.History, R.string.tab_history, Icons.Filled.History, Icons.Outlined.History),
    Tab(Route.Exercises, R.string.tab_exercises, Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
)

@Composable
fun WorkoutNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    // La séance et son récapitulatif occupent tout l'écran : pas de barre de navigation.
    val showBottomBar = tabs.any { tab -> destination?.hierarchyHasRoute(tab.route) == true }

    val settings = appContainer().settings
    val weightUnit by settings.weightUnit.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchyHasRoute(tab.route) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Route.Home) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        // Sur un écran large (tablette, pliable ouvert), on borne la largeur de
        // lecture : une colonne de texte pleine largeur devient illisible, et
        // les boutons pleine largeur deviennent absurdes.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            // Axe horizontal partagé : on entre « dans » un détail, on en ressort.
            enterTransition = { slideInHorizontally(tween(DURATION)) { it / 6 } + fadeIn(tween(DURATION)) },
            exitTransition = { slideOutHorizontally(tween(DURATION)) { -it / 6 } + fadeOut(tween(DURATION)) },
            popEnterTransition = { slideInHorizontally(tween(DURATION)) { -it / 6 } + fadeIn(tween(DURATION)) },
            popExitTransition = { slideOutHorizontally(tween(DURATION)) { it / 6 } + fadeOut(tween(DURATION)) },
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 640.dp),
        ) {
            composable<Route.Home> {
                val viewModel = containerViewModel { HomeViewModel(it.repository) }
                HomeScreen(
                    viewModel = viewModel,
                    onOpenSession = { sessionId ->
                        navController.navigate(Route.ActiveSession(sessionId))
                    },
                    onCreateWorkout = { navController.navigate(Route.WorkoutEditor()) },
                    onEditWorkout = { navController.navigate(Route.WorkoutEditor(it)) },
                    onOpenSettings = { navController.navigate(Route.Settings) },
                    onOpenExercise = { navController.navigate(Route.ExerciseDetail(it)) },
                )
            }

            composable<Route.Settings> {
                SettingsScreen(settings = settings, onBack = { navController.popBackStack() })
            }

            composable<Route.History> {
                val viewModel = containerViewModel { HistoryViewModel(it.repository, it.settings.profile) }
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenSession = { navController.navigate(Route.SessionDetail(it)) },
                )
            }

            composable<Route.Exercises> {
                val viewModel = containerViewModel { ExercisesViewModel(it.repository) }
                ExercisesScreen(
                    viewModel = viewModel,
                    onOpenExercise = { navController.navigate(Route.ExerciseDetail(it)) },
                    onCreateExercise = { navController.navigate(Route.ExerciseEditor()) },
                )
            }

            composable<Route.ActiveSession> { entry ->
                val sessionId = entry.toRoute<Route.ActiveSession>().sessionId
                val viewModel = containerViewModel(key = "session-$sessionId") {
                    ActiveSessionViewModel(it.repository, sessionId)
                }
                ActiveSessionScreen(
                    viewModel = viewModel,
                    onSessionComplete = {
                        navController.navigate(Route.SessionSummary(sessionId)) {
                            popUpTo(Route.Home)
                        }
                    },
                    onExit = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Home) { inclusive = true }
                        }
                    },
                )
            }

            composable<Route.SessionSummary> { entry ->
                val sessionId = entry.toRoute<Route.SessionSummary>().sessionId
                val viewModel = containerViewModel(key = "summary-$sessionId") {
                    SessionDetailViewModel(it.repository, sessionId, it.settings.profile)
                }
                SessionSummaryScreen(
                    viewModel = viewModel,
                    onDone = {
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Home) { inclusive = true }
                        }
                    },
                )
            }

            composable<Route.SessionDetail> { entry ->
                val sessionId = entry.toRoute<Route.SessionDetail>().sessionId
                val viewModel = containerViewModel(key = "detail-$sessionId") {
                    SessionDetailViewModel(it.repository, sessionId, it.settings.profile)
                }
                SessionDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable<Route.ExerciseDetail> { entry ->
                val exerciseId = entry.toRoute<Route.ExerciseDetail>().exerciseId
                val viewModel = containerViewModel(key = "exercise-$exerciseId") {
                    ExerciseDetailViewModel(it.repository, exerciseId)
                }
                ExerciseDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Route.ExerciseEditor(exerciseId)) },
                )
            }

            composable<Route.ExerciseEditor> { entry ->
                val exerciseId = entry.toRoute<Route.ExerciseEditor>().exerciseId
                val viewModel = containerViewModel(key = "exercise-editor-$exerciseId") {
                    ExerciseEditorViewModel(
                        it.repository,
                        it.exerciseImages,
                        exerciseId.takeIf { id -> id != Route.ExerciseEditor.NEW },
                    )
                }
                ExerciseEditorScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                    // Le détail de l'exercice n'a plus d'objet : retour direct au catalogue.
                    onDeleted = { navController.popBackStack(Route.Exercises, inclusive = false) },
                )
            }

            composable<Route.WorkoutEditor> { entry ->
                val workoutId = entry.toRoute<Route.WorkoutEditor>().workoutId
                val viewModel = containerViewModel(key = "editor-$workoutId") {
                    WorkoutEditorViewModel(
                        it.repository,
                        workoutId.takeIf { id -> id != Route.WorkoutEditor.NEW },
                    )
                }
                WorkoutEditorScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
            }
        }
        }
    }
    }
}

/** Durée « medium 2 » de l'échelle Material 3 : assez pour être perçue, jamais attendue. */
private const val DURATION = 300

private fun androidx.navigation.NavDestination.hierarchyHasRoute(route: Route): Boolean =
    hierarchy.any { it.hasRoute(route::class) }

private val androidx.navigation.NavDestination.hierarchy: Sequence<androidx.navigation.NavDestination>
    get() = generateSequence(this) { it.parent }

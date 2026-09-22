package fr.acano.workout.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.acano.workout.di.containerViewModel
import fr.acano.workout.ui.exercises.ExerciseDetailScreen
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

private data class Tab(val route: Route, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Route.Home, "Accueil", Icons.Rounded.Home),
    Tab(Route.History, "Historique", Icons.Rounded.History),
    Tab(Route.Exercises, "Exercices", Icons.Rounded.FitnessCenter),
)

@Composable
fun WorkoutNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    // La séance et son récapitulatif occupent tout l'écran : pas de barre de navigation.
    val showBottomBar = tabs.any { tab -> destination?.hierarchyHasRoute(tab.route) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
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
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable<Route.Home> {
                val viewModel = containerViewModel { HomeViewModel(it.repository) }
                HomeScreen(
                    viewModel = viewModel,
                    onOpenSession = { sessionId ->
                        navController.navigate(Route.ActiveSession(sessionId))
                    },
                )
            }

            composable<Route.History> {
                val viewModel = containerViewModel { HistoryViewModel(it.repository) }
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
                    SessionDetailViewModel(it.repository, sessionId)
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
                    SessionDetailViewModel(it.repository, sessionId)
                }
                SessionDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            composable<Route.ExerciseDetail> { entry ->
                val exerciseId = entry.toRoute<Route.ExerciseDetail>().exerciseId
                val viewModel = containerViewModel(key = "exercise-$exerciseId") {
                    ExerciseDetailViewModel(it.repository, exerciseId)
                }
                ExerciseDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun androidx.navigation.NavDestination.hierarchyHasRoute(route: Route): Boolean =
    hierarchy.any { it.hasRoute(route::class) }

private val androidx.navigation.NavDestination.hierarchy: Sequence<androidx.navigation.NavDestination>
    get() = generateSequence(this) { it.parent }

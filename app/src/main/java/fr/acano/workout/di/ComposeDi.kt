package fr.acano.workout.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.acano.workout.WorkoutApp

@Composable
fun appContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as WorkoutApp).container }
}

/** Crée un ViewModel à partir du conteneur, sans framework d'injection. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}

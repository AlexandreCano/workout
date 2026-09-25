package fr.acano.workout

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.acano.workout.timer.ActiveTimer
import fr.acano.workout.timer.TimerNotifications
import fr.acano.workout.ui.nav.WorkoutNavHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import fr.acano.workout.ui.theme.WorkoutTheme

class MainActivity : ComponentActivity() {

    /**
     * Sans cette permission, Android 13+ masque la notification du chronomètre :
     * le service de premier plan ne peut alors plus tenir le décompte écran éteint.
     */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Revenir dans l'application efface le « Repos terminé » ; et si le repos
        // se termine alors qu'on y est déjà, la notification s'efface d'elle-même
        // une fois le son et la vibration joués.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                TimerNotifications.dismissFinished(this@MainActivity)
                ActiveTimer.state.collect { timer ->
                    if (timer?.isFinished == true) {
                        // Lancé à part : une fin d'effort est aussitôt suivie d'un
                        // nouvel état (série enregistrée, chrono remis à zéro), qui ne
                        // doit pas annuler l'effacement programmé.
                        launch {
                            delay(FINISHED_NOTIFICATION_GRACE_MS)
                            TimerNotifications.dismissFinished(this@MainActivity)
                        }
                    }
                }
            }
        }

        setContent {
            WorkoutTheme {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    WorkoutNavHost()
                }
            }
        }
    }
}

/** Le temps de laisser jouer le son et la vibration de fin avant d'effacer la notification. */
private const val FINISHED_NOTIFICATION_GRACE_MS = 4_000L

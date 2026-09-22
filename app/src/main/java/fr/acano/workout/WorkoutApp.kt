package fr.acano.workout

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import fr.acano.workout.di.AppContainer
import fr.acano.workout.timer.TimerNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        TimerNotifications.createChannels(this)

        // Le programme doit exister avant le premier affichage : on sème dès le démarrage.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.repository.ensureSeeded()
        }
    }

    /** Coil ne décode les GIF / WebP animés que si le décodeur est enregistré explicitement. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(AnimatedImageDecoder.Factory()) }
            .build()
}

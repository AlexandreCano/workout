package fr.acano.workout.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerKind {
    /** Récupération entre deux séries. */
    REST,

    /** Échauffement vélo ou série de planche : c'est l'effort lui-même qui est chronométré. */
    EFFORT,
}

data class TimerState(
    val kind: TimerKind,
    val label: String,
    val totalMs: Long,
    val remainingMs: Long,
    val isRunning: Boolean,
    val isFinished: Boolean,
) {
    val remainingSeconds: Int get() = ((remainingMs + 999) / 1000).toInt()
    val progress: Float get() = if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

/**
 * Pont entre le service de premier plan (qui possède le décompte) et l'UI.
 *
 * Un singleton est ici le choix le plus simple : le service est unique par process,
 * et un `StateFlow` global évite un binding de service et son cycle de vie.
 */
object ActiveTimer {
    private val _state = MutableStateFlow<TimerState?>(null)
    val state: StateFlow<TimerState?> = _state.asStateFlow()

    internal fun update(state: TimerState?) {
        _state.value = state
    }
}
